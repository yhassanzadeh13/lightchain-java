package bootstrap;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import integration.localnet.LocalTestNet;
import model.lightchain.Identifier;
import modules.logger.LightchainLogger;
import org.slf4j.Logger;


/**
 * Bootstrap class to facilitate the generation of LightChain Nodes running on different Docker containers.
 */
public class Bootstrap {
  static final String OUTPUT_PATH = "bootstrap.txt";
  private final Logger logger = LightchainLogger.getLogger(Bootstrap.class.getCanonicalName());
  /**
   * Main method.
   *
   * @param args command-line arguments.
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

  /**
   * Writes the bootstrap file to the output file.
   *
   * @param idTable The id table.
   */
  private void writeToOutput(ConcurrentMap<Identifier, String> idTable) {
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
      this.logger.fatal("could not write to output file: " + e);
      System.exit(1);
      throw new IllegalStateException("could not read/write from/to file", e);
    }
  }
}
