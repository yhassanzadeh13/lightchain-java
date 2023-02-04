package integration.localnet;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import bootstrap.Bootstrap;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import metrics.integration.MetricsTestNet;
import modules.logger.LightchainLogger;
import modules.logger.Logger;

/**
 * Creates a metric collection network that is composed of a grafana and a prometheus container, as well as a
 * metric generation server (for now).
 * The grafana container is exposed at localhost:3000.
 * The prometheus container is exposed at localhost:9090.
 */
public class LocalTestNet extends MetricsTestNet {
  private static final String LOCAL_DOCKER_REGISTRY = "localhost:5001";
  private static final String LIGHTCHAIN_IMAGE = "/lightchain:lastest";
  private static final String SERVER_VOLUME = "server_volume";
  private static final String SERVER = "server";
  private static final String SERVER_VOLUME_BINDING = "server_volume:/app";
  private static final String DOCKER_FILE = "./Dockerfile";
  private static final String NODE_VOLUME = "server_volume";
  private static final String NODE = "server";
  private static final String NODE_VOLUME_BINDING = "server_volume:/app";
  private static final String NODE_DOCKER_FILE = "./DockerfileTestnet";

  private final Logger logger = LightchainLogger.getLogger(Bootstrap.class.getCanonicalName());

  private final int nodeCount;

  public LocalTestNet(int nodeCount) {
    this.nodeCount = nodeCount;
  }

  public LocalTestNet(int nodeCount) {
    this.nodeCount = nodeCount;
  }

  /**
   * Creates and returns HTTP Server container that serves as the local testnet.
   * Deprecated: contains a sample MVP server that is not used in the testnet.
   */
  @Deprecated
  private CreateContainerResponse createServerContainer() {
    // Volume Creation
    this.createVolumesIfNotExist(SERVER_VOLUME);

    // HTTP Server Container
    String imageId = dockerClient.buildImageCmd()
            .withDockerfile(new File(DOCKER_FILE))
            .withPull(true)
            .exec(new BuildImageResultCallback())
            .awaitImageId();

    List<Bind> serverBinds = new ArrayList<>();
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
   */
  public void runLocalTestNet() {
    this.logger.info("creating metrics collection network");
    super.runMetricsTestNet();
    this.logger.info("metrics test net created");

    this.logger.info("creating node containers");
    createNodeContainers();
    this.logger.info("node containers created");
  }

  /**
   * Creates and runs a node network accompanied by a metrics generator server.
   */
  public void createNodeContainers() {
    String imageId = LOCAL_DOCKER_REGISTRY + LIGHTCHAIN_IMAGE;
    if (dockerClient.pullImageCmd(imageId) == null) {
      // alternatively, you may run docker-build-lightchain to build the image.
      this.logger.warn("could not find image {} in local registry", imageId);
      this.logger.warn("building lightchain images from Dockerfile, this may take a while...");

      imageId = dockerClient.buildImageCmd()
          .withTags(new HashSet<>(List.of("image")))
          .withDockerfile(new File(NODE_DOCKER_FILE))
          .withPull(true)
          .exec(new BuildImageResultCallback())
          .awaitImageId();

      this.logger.info("lightchain image built, image id: {}", imageId);
    }

    List<Bind> serverBinds = new ArrayList<>();
    serverBinds.add(Bind.parse(NODE_VOLUME_BINDING));
    HostConfig hostConfig = new HostConfig()
        .withBinds(serverBinds)
        .withNetworkMode(NETWORK_NAME);
    ArrayList<CreateContainerResponse> containers = new ArrayList<>();

    for (int i = 0; i < nodeCount; i++) {
      this.createVolumesIfNotExist("NODE_VOLUME_" + i);

      logger.info("creating node container {}", i);

      CreateContainerResponse nodeServer = this.dockerClient
          .createContainerCmd(imageId)
          .withName("NODE" + i)
          .withTty(true)
          .withHostConfig(hostConfig)
          .withCmd("NODE" + i, "bootstrap.txt")
          .exec();
      containers.add(nodeServer);

      logger.info("node container {} created", i);
    }

    super.runMetricsTestNet();

    Thread[] containerThreads = new Thread[nodeCount];
    for (int i = 0; i < nodeCount; i++) {
      int finalI = i;
      containerThreads[i] = new Thread(() -> {
        this.logger.info("starting node container {}", finalI);

        dockerClient
            .startContainerCmd(containers.get(finalI).getId())
            .exec();
        this.containerLogger.registerLogger(containers.get(finalI).getId());

        this.logger.info("node container {} started", finalI);
      });
    }

    for (Thread t : containerThreads) {
      try {
        TimeUnit.MILLISECONDS.sleep(100);
      } catch (InterruptedException e) {
        logger.fatal("interrupted while sleeping to start container", e);
      }
      t.start();
    }

    while (true) {
      this.containerLogger.runContainerLoggerWorker();
      try {
        TimeUnit.SECONDS.sleep(1);
      } catch (InterruptedException e) {
        logger.fatal("interrupted while sleeping to log containers", e);
      }
    }
  }

  /**
   * Creates and runs a node network accompanied by a metrics generator server.
   */
  public void createNodeContainers() {

    // Node Container
    String imageId = dockerClient.buildImageCmd()
            .withTags(new HashSet<>(Arrays.asList("image")))
            .withDockerfile(new File(NODE_DOCKER_FILE))
            .withPull(true)
            .exec(new BuildImageResultCallback())
            .awaitImageId();

    List<Bind> serverBinds = new ArrayList<>();
    serverBinds.add(Bind.parse(NODE_VOLUME_BINDING));

    HostConfig hostConfig = new HostConfig()
            .withBinds(serverBinds)
            .withNetworkMode(NETWORK_NAME);

    ArrayList<CreateContainerResponse> containers = new ArrayList<>();

    for (int i = 0; i < nodeCount; i++) {
      // Volume Creation
      this.createVolumesIfNotExist("NODE_VOLUME_" + i);

      System.out.println("building local node " + i + " , please wait ....");

      CreateContainerResponse nodeServer = this.dockerClient
              .createContainerCmd(imageId)
              .withName("NODE" + i)
              .withTty(true)
              .withHostConfig(hostConfig)
              .withCmd("NODE" + i, "bootstrap.txt")
              .exec();
      containers.add(nodeServer);
    }

    super.runMetricsTestNet();

    Thread[] containerThreads = new Thread[nodeCount];
    for (int i = 0; i < nodeCount; i++) {
      int finalI = i;
      containerThreads[i] = new Thread(() -> {
        try {
          TimeUnit.MILLISECONDS.sleep(1000);
        } catch (InterruptedException e) {
          System.err.println("thread operation interrupted: " + e);
        }

        dockerClient
                .startContainerCmd(containers.get(finalI).getId())
                .exec();

        dockerClient
                .logContainerCmd(containers.get(finalI).getId())
                .withStdErr(true)
                .withStdOut(true)
                .withFollowStream(true)
                .withSince((int) (System.currentTimeMillis() / 1000))
                .exec(new ResultCallbackTemplate<LogContainerResultCallback, Frame>() {
                  @Override
                  public void onNext(Frame frame) {
                    System.out.print("Node " + finalI + "> " + new String(frame.getPayload(), StandardCharsets.UTF_8));
                  }
                });
        System.out.println("Node " + finalI + " is up and running!");
        while (true) {
        }
      });
    }

    for (Thread t : containerThreads) {
      t.start();
    }
  }

  /**
   * Builds and returns a ConcurrentMap of nodes to be bootstrapped.
   */
  public ConcurrentMap createBootstrapFile() {
    ConcurrentMap<Identifier, String> idTable = new ConcurrentHashMap<>();

    for (int i = 0; i < nodeCount; i++) {
      Identifier id = new Identifier(("NODE" + i).getBytes(StandardCharsets.UTF_8));
      idTable.put(id, "NODE" + i + ":8081");
    }
    return idTable;
  }
}


