package bootstrap;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import model.lightchain.Identifier;
import modules.logger.LightchainLogger;
import modules.logger.Logger;

public class Bootstrap {
  private final String BOOTSTRAP_FILE_NAME = "bootstrap.txt"; // Don't change this name, it is used in the Dockerfile.
  private final String BOOTSTRAP_KEY_NAME = "node";

  private final String BOOTSTRAP_PORT_NUMBER = "8081";

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
      Identifier id = new Identifier((BOOTSTRAP_KEY_NAME + i).getBytes(StandardCharsets.UTF_8));
      idTable.put(id, BOOTSTRAP_KEY_NAME + i + ":" + BOOTSTRAP_PORT_NUMBER);
    }
    return idTable;
  }

  /**
   * Writes the bootstrap file to the output file.
   *
   * @param idTable the id table to be written to the output file.
   */
  private void writeOnFile(HashMap<Identifier, String> idTable) {
    File file = new File(BOOTSTRAP_FILE_NAME);
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

  public void print(HashMap<Identifier, String> idTable) {
    logger.info("bootstrap file created with the following content:");
    for (Map.Entry<Identifier, String> id : idTable.entrySet()) {
      logger.info(id.getKey() + " " + idTable.get(id.getKey()));
    }
    logger.info("bootstrap file written to " + BOOTSTRAP_FILE_NAME);
  }
}
