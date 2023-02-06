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
