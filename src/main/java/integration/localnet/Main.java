package integration.localnet;

import java.io.File;
import java.time.*;
import java.util.*;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import metrics.integration.MetricsTestNet;

/**
 *  Main Runner Class for orchestrating the building of the HTTP Server, Prometheus, and Grafana
 *  components of the TestNet. This class also utilizes the Java Docker API in order to containerize
 *  the three components and build up the necessary supporting infrastructure.
 */
public class Main {

  private static final int SERVER_PORT = 8081;
  private static final String SERVER_VOLUME = "server_volume";
  private static final String NETWORK_NAME = "network";
  private static final String SERVER = "server";
  private static final String SERVER_VOLUME_BINDING = "server_volume:/app";

  /** main function.
   *
   * @param args standart Java args
   */
  public static void main(String[] args) {

    MetricsTestNet testNet = new MetricsTestNet();

    try {
      testNet.run();
    } catch (IllegalStateException e) {
      System.err.println("could not run metrics testnet" + e);
      System.exit(1);
    }

    // HTTP Server Container

    try {
      createServerContainer();
    } catch (IllegalStateException e) {
      System.err.println("could not initialize and run HTTP Server Container" + e);
      System.exit(1);
    }

  }

  /**
   * Creates and returns a HTTP Server container.
   *
   * @throws IllegalStateException when container creation faces an illegal state.
   */
  private static void createServerContainer() {

    // Docker Client

    DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

    DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .maxConnections(100)
            .connectionTimeout(Duration.ofSeconds(30))
            .responseTimeout(Duration.ofSeconds(45))
            .build();

    DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);

    // Volume Creation

    ListVolumesResponse volumesResponse = dockerClient.listVolumesCmd().exec();
    List<InspectVolumeResponse> volumes = volumesResponse.getVolumes();
    boolean serverVolumeExists = false;

    for (InspectVolumeResponse v : volumes) {
      if (v.getName().equals(SERVER_VOLUME)) {
        // volume exists
        serverVolumeExists = true;
      }
    }

    // volume name does not exist, create one.
    if (!serverVolumeExists) {
      dockerClient.createVolumeCmd().withName(SERVER_VOLUME).exec();
    }

    // HTTP Server Container

    String imageId = dockerClient.buildImageCmd()
            .withDockerfile(new File("./Dockerfile"))
            .withPull(true)
            .exec(new BuildImageResultCallback())
            .awaitImageId();

    Ports serverPortBindings = new Ports();
    serverPortBindings.bind(ExposedPort.tcp(SERVER_PORT), Ports.Binding.bindPort(SERVER_PORT));

    List<Bind> serverBinds = new ArrayList<Bind>();
    serverBinds.add(Bind.parse(SERVER_VOLUME_BINDING));

    HostConfig hostConfig = new HostConfig()
            .withBinds(serverBinds)
            .withNetworkMode(NETWORK_NAME)
            .withPortBindings(serverPortBindings);

    CreateContainerResponse serverContainer = dockerClient
            .createContainerCmd(imageId)
            .withName(SERVER)
            .withTty(true)
            .withHostConfig(hostConfig)
            .exec();

    dockerClient
            .startContainerCmd(serverContainer.getId())
            .exec();

  }

}
