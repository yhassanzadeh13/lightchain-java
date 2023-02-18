package integration.localnet;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import bootstrap.Bootstrap;
import bootstrap.BootstrapInfo;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
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

  private final BootstrapInfo bootstrapInfo;

  private final Logger logger = LightchainLogger.getLogger(Bootstrap.class.getCanonicalName());

  public LocalTestNet(BootstrapInfo info) {
    this.bootstrapInfo = info;
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

    HostConfig hostConfig = new HostConfig().withBinds(serverBinds).withNetworkMode(NETWORK_NAME);

    return this.dockerClient.createContainerCmd(imageId).withName(SERVER).withTty(true).withHostConfig(hostConfig).exec();
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
    HostConfig hostConfig = new HostConfig().withBinds(serverBinds).withNetworkMode(NETWORK_NAME);
    ArrayList<CreateContainerResponse> containers = new ArrayList<>();

    for (int i = 0; i < this.bootstrapInfo.size(); i++) {
      this.createVolumesIfNotExist("NODE_VOLUME_" + i);

      logger.info("creating node container {}", i);

      CreateContainerResponse nodeServer = this.dockerClient.createContainerCmd(imageId)
          .withName("NODE" + i)
          .withTty(true)
          .withHostConfig(hostConfig)
          .withCmd(bootstrapInfo.getIdentifier(i).toString(), bootstrapInfo.getBootstrapFileName())
          .exec();
      containers.add(nodeServer);

      logger.info("node container {} created", i);
    }

    super.runMetricsTestNet();

    Thread[] containerThreads = new Thread[this.bootstrapInfo.size()];
    Thread[] containerLoggerThreads = new Thread[this.bootstrapInfo.size()];
    for (int i = 0; i < this.bootstrapInfo.size(); i++) {
      int finalI = i;
      containerThreads[i] = new Thread(() -> {
        this.logger.info("starting node container {}", finalI);

        dockerClient.startContainerCmd(containers.get(finalI).getId()).exec();
        this.logger.info("node container {} started", finalI);
      });

      containerLoggerThreads[i] = new Thread(() -> {
        this.logger.info("starting node container logger {}", finalI);

        this.containerLogger.runContainerLoggerWorker(containers.get(finalI).getId());
        this.logger.info("node container logger {} started", finalI);
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

    for (Thread t : containerLoggerThreads) {
      try {
        TimeUnit.MILLISECONDS.sleep(100);
      } catch (InterruptedException e) {
        logger.fatal("interrupted while sleeping to start container logger", e);
      }
      t.start();
    }

    for (Thread t : containerThreads) {
      try {
        t.join();
      } catch (InterruptedException e) {
        logger.fatal("interrupted while waiting for container to start", e);
      }
    }
  }
}


