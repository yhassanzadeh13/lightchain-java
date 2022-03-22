package metrics.integration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectVolumeResponse;
import com.github.dockerjava.api.command.ListVolumesResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

public class MetricsTestNet {
  private static final String PROMETHEUS_VOLUME = "prometheus_volume";
  private static final String GRAFANA_VOLUME = "grafana_volume";
  private static final String NETWORK_NAME = "network";
  private static final String NETWORK_DRIVER_NAME = "bridge";


  // Grafana
  private static final String GRAFANA_VOLUME_BINDING = "grafana_volume:/var/lib/grafana";
  private static final String GRAFANA_DASHBOARD_BINDING = "/grafana/provisioning/dashboards:/etc/grafana/provisioning/dashboards";
  private static final String GRAFANA_DATA_SOURCE_BINDING = "/grafana/provisioning/datasources:/etc/grafana/provisioning/datasources";


  private final DockerClient dockerClient;

  public MetricsTestNet() {
    DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

    DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
        .dockerHost(config.getDockerHost())
        .sslConfig(config.getSSLConfig())
        .maxConnections(100)
        .connectionTimeout(Duration.ofSeconds(30))
        .responseTimeout(Duration.ofSeconds(45))
        .build();

    this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
  }

  public void run() throws IllegalStateException {
    // Volume check + create if absent
    this.createVolumesIfNotExist(dockerClient, PROMETHEUS_VOLUME);
    this.createVolumesIfNotExist(dockerClient, GRAFANA_VOLUME);

    // Network
    this.createNetworkIfNotExist();

    // Prometheus
    try {
      dockerClient.pullImageCmd("prom/prometheus")
          .withTag("main")
          .exec(new PullImageResultCallback())
          .awaitCompletion(60, TimeUnit.SECONDS);
    } catch (InterruptedException ex) {
      throw new IllegalStateException("could not run prometheus container" + ex);
    }

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
    CreateContainerResponse grafanaContainer = this.createGrafanaContainer();

    dockerClient
        .startContainerCmd(grafanaContainer.getId())
        .exec();
  }

  /**
   * Checks for existence of given volume name in the client, and creates one with the
   * given name if volume name does not exist.
   *
   * @param client     docker client.
   * @param volumeName volume name to create.
   */
  private void createVolumesIfNotExist(DockerClient client, String volumeName) {
    ListVolumesResponse volumesResponse = client.listVolumesCmd().exec();
    List<InspectVolumeResponse> volumes = volumesResponse.getVolumes();

    for (InspectVolumeResponse v : volumes) {
      if (v.getName().equals(volumeName)) {
        // volume exists
        return;
      }
    }

    // volume name does not exist, create one.
    client.createVolumeCmd().withName(volumeName).exec();
  }

  /**
   * Checks for existence of the given network in the client, and creates one with the given name
   * if the network does not exist.
   */
  private void createNetworkIfNotExist() {
    List<Network> networks = this.dockerClient.listNetworksCmd().exec();

    for (Network n : networks) {
      if (n.getName().equals(NETWORK_NAME)) {
        // network exists
        return;
      }
    }

    // network does not exist, create one/
    dockerClient.createNetworkCmd().withName(NETWORK_NAME).withDriver(MetricsTestNet.NETWORK_NAME).exec();
  }


  private CreateContainerResponse createGrafanaContainer() throws InterruptedException {
    try {
      this.dockerClient.pullImageCmd("grafana/grafana")
          .withTag("main")
          .exec(new PullImageResultCallback())
          .awaitCompletion(60, TimeUnit.SECONDS);
    } catch (InterruptedException ex) {
      throw new IllegalStateException("could not run grafana container" + ex);
    }

    Ports grafanaPortBindings = new Ports();
    grafanaPortBindings.bind(ExposedPort.tcp(3000), Ports.Binding.bindPort(3000));

    List<Bind> grafBinds = new ArrayList<Bind>();
    grafBinds.add(Bind.parse(GRAFANA_VOLUME_BINDING));
    grafBinds.add(Bind.parse(System.getProperty("user.dir") + GRAFANA_DASHBOARD_BINDING));
    grafBinds.add(Bind.parse(System.getProperty("user.dir") + GRAFANA_DATA_SOURCE_BINDING));

    HostConfig hostConfig = new HostConfig()
        .withBinds(grafBinds)
        .withNetworkMode(NETWORK_NAME)
        .withPortBindings(grafanaPortBindings);

    return this.dockerClient
        .createContainerCmd("grafana/grafana:main")
        .withName("grafana")
        .withTty(true)
        .withEnv("GF_SECURITY_ADMIN_USER=${ADMIN_USER:-admin}")
        .withEnv("GF_SECURITY_ADMIN_PASSWORD=${ADMIN_PASSWORD:-admin}")
        .withEnv("GF_USERS_ALLOW_SIGN_UP=false")
        .withHostConfig(hostConfig)
        .exec();
  }


}
