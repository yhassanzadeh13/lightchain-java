package bootstrap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import metrics.collectors.MetricServer;
import model.lightchain.Identifier;
import modules.logger.LightchainLogger;
import modules.logger.Logger;

/**
 * Bootstrap is a class that creates a bootstrap file for the nodes. The bootstrap file is a text file
 * that contains the nodes to be bootstrapped. Each line of the file contains a node identifier and
 * the node's address.
 */
public class Bootstrap {
  private static final Random random = new Random();
  private final String bootstrapFileName = "bootstrap.txt"; // Don't change this name, it is used in the Dockerfile.
  private final String bootstrapKeyName = "node";
  /**
   * Port number at which the node will be listening for incoming connections from other nodes.
   */
  public static final int bootstrapPortNumber = 8082;
  private final Logger logger = LightchainLogger.getLogger(Bootstrap.class.getCanonicalName());
  private final short nodeCount;

  /**
   * The docker names is a list that contains the names of the docker containers.
   * This list is used to create containers with the same name on the LocalTestNet.
   * This is necessary for containers to have the same name as their address in this bootstrap id table.
   * Otherwise, the containers may not be able to find each other on the LocalTestNet docker network.
   */
  private final List<String> dockerNames = new ArrayList<>();

  private final List<Identifier> identifiers = new ArrayList<>();

  /**
   * The id table is a map that contains the node's identifier and the node's address.
   */
  private final HashMap<Identifier, String> idTable;
  /**
   * The metrics table is a map that contains the node's identifier and the node's metrics address.
   */
  private final HashMap<Identifier, String> metricsTable;

  /**
   * Constructor for Bootstrap.
   *
   * @param nodeCount number of nodes to be created.
   */
  public Bootstrap(short nodeCount) {
    this.nodeCount = nodeCount;
    this.idTable = new HashMap<>();
    this.metricsTable = new HashMap<>();
  }

  /**
   * Builds the bootstrap file. The bootstrap file is a text file that contains the nodes to be
   * bootstrapped. Each line of the file contains a node identifier and the node's address.
   * The bootstrap file is written to the output file.
   */
  public BootstrapInfo build() {
    this.makeBootstrap();
    this.writeOnFile();
    this.print();
    return new BootstrapInfo(this.identifiers, this.dockerNames, this.bootstrapFileName, this.idTable, this.metricsTable);
  }

  /**
   * Builds and returns a ConcurrentMap of nodes to be bootstrapped.
   */
  private void makeBootstrap() {
    for (int i = 0; i < this.nodeCount; i++) {
      Identifier id = this.newIdentifier();
      while (idTable.containsKey(id)) {
        this.logger.warn("id {} already exists, generating a new one", id);
        id = this.newIdentifier();
      }
      String dockerName = bootstrapKeyName.toLowerCase() + i;
      this.dockerNames.add(dockerName);
      this.identifiers.add(id);
      this.idTable.put(id, dockerName + ":" + bootstrapPortNumber);
      // TODO: server port should be a parameter.
      this.metricsTable.put(id, dockerName + ":" + MetricServer.SERVER_PORT);
    }
  }

  /**
   * Writes the bootstrap file to the output file.
   */
  private void writeOnFile() {
    File file = new File(bootstrapFileName);
    try {
      FileOutputStream fileStream = new FileOutputStream(file);
      Writer writer = new OutputStreamWriter(fileStream, StandardCharsets.UTF_8);
      for (Map.Entry<Identifier, String> id : this.idTable.entrySet()) {
        writer.write(id.getKey() + ":" + this.idTable.get(id.getKey()) + "\n");
      }
      writer.flush();
      writer.close();
    } catch (IOException e) {
      this.logger.fatal("could not write bootstrap file", e);
    }
  }

  /**
   * Prints the id table on the console.
   */
  public void print() {
    logger.info("bootstrap file created with the following content:");
    for (Map.Entry<Identifier, String> id : this.idTable.entrySet()) {
      logger.info(id.getKey() + " " + this.idTable.get(id.getKey()));
    }
    logger.info("bootstrap file written to " + bootstrapFileName);
  }

  /**
   * Generates a random identifier.
   * Note: this is a temporary method to generate random identifiers. In mature LightChain, the Identifier of a node
   * is the hash of the public key of the node.
   *
   * @return a random identifier.
   */
  private model.lightchain.Identifier newIdentifier() {
    byte[] bytes = new byte[model.lightchain.Identifier.Size];
    random.nextBytes(bytes);
    return new model.lightchain.Identifier(bytes);
  }
}
