package integration.localnet;

import java.io.File;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

/**
 *  Main Runner Class for orchestrating the building of the HTTP Server, Prometheus, and Grafana
 *  components of the TestNet. This class also utilizes the Java Docker API in order to containerize
 *  the three components and build up the necessary supporting infrastructure.
 */
public class Main {

  /** main function.
   *
   * @param args standart Java args
   */
  public static void main(String[] args) throws InterruptedException {

    DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

    DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .maxConnections(100)
            .connectionTimeout(Duration.ofSeconds(30))
            .responseTimeout(Duration.ofSeconds(45))
            .build();

    DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);

    // Volume check + create if absent

    ListVolumesResponse volumesResponse = dockerClient.listVolumesCmd().exec();
    List<InspectVolumeResponse> volumes = volumesResponse.getVolumes();

    boolean serverVolumeFound = false;
    boolean promVolumeFound = false;
    boolean grafVolumeFound = false;

    for (InspectVolumeResponse v : volumes) {
      if (v.getName().equals("server_volume")) {
        serverVolumeFound = true;
      } else if (v.getName().equals("prometheus_volume")) {
        promVolumeFound = true;
      } else if (v.getName().equals("grafana_volume")) {
        grafVolumeFound = true;
      }
    }

    if (!serverVolumeFound) {
      CreateVolumeResponse volume = dockerClient.createVolumeCmd().withName("server_volume").exec();
    }

    if (!promVolumeFound) {
      CreateVolumeResponse volume = dockerClient.createVolumeCmd().withName("prometheus_volume").exec();
    }

    if (!grafVolumeFound) {
      CreateVolumeResponse volume = dockerClient.createVolumeCmd().withName("grafana_volume").exec();
    }

    // Network

    List<Network> networks = dockerClient.listNetworksCmd().exec();
    boolean networkFound = false;

    for (Network n : networks) {
      if (n.getName().equals("network")) {
        networkFound = true;
      }
    }

    if (!networkFound) {
      CreateNetworkResponse networkResponse
              = dockerClient.createNetworkCmd()
              .withName("network")
              .withDriver("bridge")
              .exec();
    }

    // HTTP Server

    String imageId = dockerClient.buildImageCmd()
            .withDockerfile(new File("./Dockerfile"))
            .withPull(true)
            .exec(new BuildImageResultCallback())
            .awaitImageId();

    Ports serverPortBindings = new Ports();
    serverPortBindings.bind(ExposedPort.tcp(8080), Ports.Binding.bindPort(8080));

    CreateContainerResponse serverContainer =
            dockerClient
                    .createContainerCmd(imageId)
                    .withBinds(Bind.parse("server_volume" + ":" + "/app"))
                    .withName("server")
                    .withNetworkMode("network")
                    .withTty(true)
                    .withPortBindings(serverPortBindings)
                    .exec();

    dockerClient
            .startContainerCmd(serverContainer.getId())
            .exec();

    // Prometheus

    dockerClient.pullImageCmd("prom/prometheus")
            .withTag("main")
            .exec(new PullImageResultCallback())
            .awaitCompletion(60, TimeUnit.SECONDS);

    Ports promPortBindings = new Ports();
    promPortBindings.bind(ExposedPort.tcp(9090), Ports.Binding.bindPort(9090));

    List<Bind> promBinds = new ArrayList<Bind>();
    promBinds.add(Bind.parse(System.getProperty("user.dir") + "/prometheus" + ":" + "/etc/prometheus"));
    promBinds.add(Bind.parse("prometheus_volume" + ":" + "/prometheus"));

    CreateContainerResponse promContainer =
            dockerClient
                    .createContainerCmd("prom/prometheus:main")
                    .withBinds(promBinds)
                    .withName("prometheus")
                    .withNetworkMode("network")
                    .withTty(true)
                    .withPortBindings(promPortBindings)
                    .exec();

    dockerClient
            .startContainerCmd(promContainer.getId())
            .exec();

    // Grafana

    dockerClient.pullImageCmd("grafana/grafana")
            .withTag("main")
            .exec(new PullImageResultCallback())
            .awaitCompletion(60, TimeUnit.SECONDS);

    Ports grafanaPortBindings = new Ports();
    grafanaPortBindings.bind(ExposedPort.tcp(3000), Ports.Binding.bindPort(3000));

    List<Bind> grafBinds = new ArrayList<Bind>();
    grafBinds.add(Bind.parse("grafana_volume" + ":" + "/var/lib/grafana"));
    grafBinds.add(Bind.parse(System.getProperty("user.dir") + "/grafana/provisioning/dashboards"
            + ":" + "/etc/grafana/provisioning/dashboards"));
    grafBinds.add(Bind.parse(System.getProperty("user.dir") + "/grafana/provisioning/datasources"
            + ":" + "/etc/grafana/provisioning/datasources"));

    CreateContainerResponse grafanaContainer =
            dockerClient
                    .createContainerCmd("grafana/grafana:main")
                    .withBinds(grafBinds)
                    .withName("grafana")
                    .withNetworkMode("network")
                    .withTty(true)
                    .withPortBindings(grafanaPortBindings)
                    .withEnv("GF_SECURITY_ADMIN_USER=${ADMIN_USER:-admin}")
                    .withEnv("GF_SECURITY_ADMIN_PASSWORD=${ADMIN_PASSWORD:-admin}")
                    .withEnv("GF_USERS_ALLOW_SIGN_UP=false")
                    .exec();

    dockerClient
            .startContainerCmd(grafanaContainer.getId())
            .exec();

  }

}
