package integration.localnet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectVolumeResponse;
import com.github.dockerjava.api.command.ListVolumesResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import metrics.integration.MetricsTestNet;

/**
 * Creates a metric collection network that is composed of a grafana and a prometheus container, as well as a
 * metric generation server (for now).
 * The grafana container is exposed at localhost:3000.
 * The prometheus container is exposed at localhost:9090.
 */
public class LocalTestNet extends MetricsTestNet {
  private static final String SERVER_VOLUME = "server_volume";
  private static final String SERVER = "server";
  private static final String SERVER_VOLUME_BINDING = "server_volume:/app";
  private static final String DOCKER_FILE = "./Dockerfile";

  public LocalTestNet() {
    super();
  }

  /**
   * Creates and returns HTTP Server container.
   *
   * @throws IllegalStateException when container creation faces an illegal state.
   */
  private CreateContainerResponse createServerContainer() throws IllegalStateException {
    // Volume Creation
    ListVolumesResponse volumesResponse = this.dockerClient.listVolumesCmd().exec();
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
      this.dockerClient.createVolumeCmd().withName(SERVER_VOLUME).exec();
    }

    // HTTP Server Container
    String imageId = dockerClient.buildImageCmd()
        .withDockerfile(new File(DOCKER_FILE))
        .withPull(true)
        .exec(new BuildImageResultCallback())
        .awaitImageId();

    List<Bind> serverBinds = new ArrayList<Bind>();
    serverBinds.add(Bind.parse(SERVER_VOLUME_BINDING));

    HostConfig hostConfig = new HostConfig()
        .withBinds(serverBinds)
        .withNetworkMode(NETWORK_NAME);

    return this.dockerClient
        .createContainerCmd(imageId)
        .withName(SERVER)
        .withTty(true)
        .withHostConfig(hostConfig)
        .exec();
  }

  /**
   * Creates and runs a metrics collection network accompanied by a metrics generator server.
   *
   * @throws IllegalStateException when container creation faces an illegal state.
   */
  public void runLocalTestNet() throws IllegalStateException {
    super.runMetricsTestNet();

    CreateContainerResponse httpServer = this.createServerContainer();
    dockerClient
        .startContainerCmd(httpServer.getId())
        .exec();

    System.out.println("localnet is up and running");
  }
}


