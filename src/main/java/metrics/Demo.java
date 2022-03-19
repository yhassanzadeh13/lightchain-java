package metrics;

import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.github.dockerjava.api.*;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.*;
import com.github.dockerjava.httpclient5.*;
import com.github.dockerjava.transport.*;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;

/**
 * Demonstrative class to set up a Prometheus server and create LightChain Counter and Gauge instances.
 */
public class Demo {

  static LightChainCollector collector;
  static Counter finalizedBlockCount;
  static Gauge currentBlockCount;

  /**
   * main function.
   *
   * @param args standart Java args
   */
  public static void main(String[] args) {

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

    boolean promVolumeFound = false;
    boolean grafVolumeFound = false;

    for (InspectVolumeResponse v : volumes) {
      if (v.getName().equals("prometheus_volume")) {
        promVolumeFound = true;
      } else if (v.getName().equals("grafana_volume")) {
        grafVolumeFound = true;
      }
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

    // Prometheus
    try {
      dockerClient.pullImageCmd("prom/prometheus")
              .withTag("main")
              .exec(new PullImageResultCallback())
              .awaitCompletion(60, TimeUnit.SECONDS);
    } catch (InterruptedException ex) {
      System.err.println("Interrupted Exception: Prometheus image could not be pulled, exiting program");
      System.exit(1);
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
    try {
      dockerClient.pullImageCmd("grafana/grafana")
              .withTag("main")
              .exec(new PullImageResultCallback())
              .awaitCompletion(60, TimeUnit.SECONDS);
    } catch (InterruptedException ex) {
      System.err.println("Interrupted Exception: Grafana image could not be pulled, exiting program");
      System.exit(1);
    }

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

    // Metric Server Initiation

    try {

      collector = new LightChainCollector();

      finalizedBlockCount = collector.counter().register("finalized_block_count",
              "consensus", "proposal", "Finalized block count");

      currentBlockCount = collector.gauge().register("current_block_count",
              "consensus", "proposal", "Finalized block count");

    } catch (IllegalArgumentException ex) {
      System.err.println("Could not initialize the metrics with the provided arguments" + ex.toString());
      System.exit(1);
    }

    try {
      MetricServer.start();
    } catch (IllegalStateException ex) {
      System.err.println("Could not start the Metric Server");
      System.exit(1);
    }

    while (true) {
      try {
        Thread.sleep(1000);
        finalizedBlockCount.inc(1);
        currentBlockCount.inc(1);
      } catch (InterruptedException ex) {
        System.err.println("Thread sleep issue, breaking the loop");
        break;
      }
    }

    try {
      MetricServer.terminate();
    } catch (IllegalStateException ex) {
      System.err.println("Could not stop the Metric Server");
      System.exit(1);
    }

  }

}
