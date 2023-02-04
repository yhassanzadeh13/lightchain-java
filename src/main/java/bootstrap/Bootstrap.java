package bootstrap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import model.lightchain.Identifier;
import modules.logger.LightchainLogger;
import modules.logger.Logger;

/**
 * Bootstrap is a class that creates a bootstrap file for the nodes. The bootstrap file is a text file
 * that contains the nodes to be bootstrapped. Each line of the file contains a node identifier and
 * the node's address.
 */
public class Bootstrap {
  private final String bootstrapFileName = "bootstrap.txt"; // Don't change this name, it is used in the Dockerfile.
  private final String bootstrapKeyName = "node";

  private final String bootstrapPortNumber = "8081";

  private final Logger logger = LightchainLogger.getLogger(Bootstrap.class.getCanonicalName());

  private final short nodeCount;

  /**
   * Constructor for Bootstrap.
   *
   * @param nodeCount number of nodes to be created.
   */
  public Bootstrap(short nodeCount) {
    this.nodeCount = nodeCount;
  }

  /**
   * Builds the bootstrap file. The bootstrap file is a text file that contains the nodes to be
   * bootstrapped. Each line of the file contains a node identifier and the node's address.
   * The bootstrap file is written to the output file.
   */
  public void build() {
    HashMap<Identifier, String> idTable = this.createBootstrapFile();
    this.writeOnFile(idTable);
    this.print(idTable);
  }

  /**
   * Builds and returns a ConcurrentMap of nodes to be bootstrapped.
   */
  private HashMap<Identifier, String> createBootstrapFile() {
    HashMap<Identifier, String> idTable = new HashMap<>();

    for (int i = 0; i < this.nodeCount; i++) {
      // TODO: generate a real random identifier.
      Identifier id = new Identifier((bootstrapKeyName + i).getBytes(StandardCharsets.UTF_8));
      idTable.put(id, bootstrapKeyName + i + ":" + bootstrapPortNumber);
    }
    return idTable;
  }

  /**
   * Writes the bootstrap file to the output file.
   *
   * @param idTable the id table to be written to the output file.
   */
  private void writeOnFile(HashMap<Identifier, String> idTable) {
    File file = new File(bootstrapFileName);
    try {
      FileOutputStream fileStream = new FileOutputStream(file);
      Writer writer = new OutputStreamWriter(fileStream, StandardCharsets.UTF_8);
      for (Map.Entry<Identifier, String> id : idTable.entrySet()) {
        writer.write(id.getKey() + ":" + idTable.get(id.getKey()) + "\n");
      }
      writer.flush();
      writer.close();
    } catch (IOException e) {
      this.logger.fatal("could not write bootstrap file", e);
    }
  }

  /**
   * Prints the bootstrap file to the console.
   *
   * @param idTable the id table to be printed.
   */
  public void print(HashMap<Identifier, String> idTable) {
    logger.info("bootstrap file created with the following content:");
    for (Map.Entry<Identifier, String> id : idTable.entrySet()) {
      logger.info(id.getKey() + " " + idTable.get(id.getKey()));
    }
    logger.info("bootstrap file written to " + bootstrapFileName);
  }
}

package bootstrap;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.gson.Gson;
import integration.localnet.LocalTestNet;
import model.lightchain.Identifier;

/**
 * Bootstrap class to facilitate the generation of LightChain Nodes running on different Docker containers.
 */
public class Bootstrap {
  static final String OUTPUT_PATH = "bootstrap.txt";
  static final String PROM_CONFIG_PATH = "prometheus/nodes.json";

  /**
   * Main method.
   *
   * @param args Arguments.
   */
  public static void main(String[] args) {
    int nodeCount = 10;
    ConcurrentMap<Identifier, String> idTable;
    LocalTestNet testNet = new LocalTestNet(nodeCount);

    try {
      idTable = testNet.createBootstrapFile();
      writeToOutput(idTable);
      testNet.createNodeContainers();

      for (Map.Entry<Identifier, String> id : idTable.entrySet()) {
        System.out.println(id.getKey().toString() + " " + idTable.get(id.getKey()));
      }

    } catch (IllegalStateException e) {
      System.exit(1);
      throw new IllegalStateException("could not initialize and run the Node network", e);
    }
  }

  private static ConcurrentMap<Identifier, String> idTableModifierForMetricsServerPort(
          ConcurrentMap<Identifier, String> idTable) {
    ConcurrentMap modifiedMap = new ConcurrentHashMap<Identifier, String>();

    for (Map.Entry<Identifier, String> id : idTable.entrySet()) {
      modifiedMap.put(id.getKey(), idTable.get(id.getKey()).split(":")[0] + ":8082");
    }

    return modifiedMap;

  }

  /**
   * Writes the bootstrap file to the output file.
   *
   * @param idTable The id table.
   */
  private static void writeToOutput(ConcurrentMap<Identifier, String> idTable) {
    File file = new File(OUTPUT_PATH);
    try {
      FileOutputStream fileStream = new FileOutputStream(file);
      Writer writer = new OutputStreamWriter(fileStream, StandardCharsets.UTF_8);
      for (Map.Entry<Identifier, String> id : idTable.entrySet()) {
        writer.write(id.getKey() + ":" + idTable.get(id.getKey()) + "\n");
      }
      writer.flush();
      writer.close();
    } catch (IOException e) {
      System.exit(1);
      throw new IllegalStateException("could not read/write from/to file", e);
    }
  }

  private static void writeTargetJson(ConcurrentMap<Identifier, String> idTable) {
    try {
      Map<String, Object> nodes = new HashMap<>();
      Map<String, Object> labels = new HashMap<>();
      labels.put("job", "nodes");
      labels.put("env", "dev");
      nodes.put("targets", idTable.values());
      nodes.put("labels", labels);

      Gson gson = new Gson();
      Map[] pm = new Map[1];
      pm[0] = nodes;
      BufferedWriter writer = Files.newBufferedWriter(Paths.get(PROM_CONFIG_PATH));

      writer.write(gson.toJson(pm));
      writer.close();
    } catch (IOException e) {
      throw new IllegalStateException("could not read/write from/to file", e);
    }
  }
}
