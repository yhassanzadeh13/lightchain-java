package integration.localnet;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import metrics.integration.MetricsTestNet;
import model.lightchain.Identifier;

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

  private static final String NODE_VOLUME = "server_volume";
  private static final String NODE = "server";
  private static final String NODE_VOLUME_BINDING = "server_volume:/app";
  private static final String NODE_DOCKER_FILE = "./DockerfileNode";

  private int nodeCount;

  public LocalTestNet() {
    super();
  }

  public LocalTestNet(int nodeCount) {
    this.nodeCount=nodeCount;
  }

  /**
   * Creates and returns HTTP Server container that serves as the local testnet.
   *
   * @throws IllegalStateException when container creation faces an illegal state.
   */
  private CreateContainerResponse createServerContainer() throws IllegalStateException {
    // Volume Creation
    this.createVolumesIfNotExist(SERVER_VOLUME);

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

    System.out.println("building local testnet, please wait ....");

    CreateContainerResponse httpServer = this.createServerContainer();
    dockerClient
        .startContainerCmd(httpServer.getId())
        .exec();

    System.out.println("local testnet is up and running!");
  }

  public HashMap createNodeContainers() throws IllegalStateException {

    HashMap<Identifier, String> idTable = new HashMap<Identifier, String>();

    super.runMetricsTestNet();

    // Node Container
    String imageId = dockerClient.buildImageCmd()
            .withDockerfile(new File(NODE_DOCKER_FILE))
            .withPull(true)
            .exec(new BuildImageResultCallback())
            .awaitImageId();

    List<Bind> serverBinds = new ArrayList<Bind>();
    serverBinds.add(Bind.parse(NODE_VOLUME_BINDING));

    HostConfig hostConfig = new HostConfig()
            .withBinds(serverBinds)
            .withNetworkMode(NETWORK_NAME);

    for (int i = 0; i < nodeCount; i++) {
      // Volume Creation
      this.createVolumesIfNotExist("NODE_VOLUME_"+i);

      System.out.println("building local node " + i + " , please wait ....");

      CreateContainerResponse nodeServer = this.dockerClient
              .createContainerCmd(imageId)
              .withName("NODE"+i)
              .withTty(true)
              .withHostConfig(hostConfig)
              .exec();

      dockerClient
              .startContainerCmd(nodeServer.getId())
              .exec();

      System.out.println("Node " + i + " is up and running! at address: ");

    }

    return idTable;

  }

}


