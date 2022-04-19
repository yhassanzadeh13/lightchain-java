package bootstrap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ConcurrentMap;

import integration.localnet.LocalTestNet;
import model.lightchain.Identifier;

/**
 * Bootstrap class to facilitate the generation of LightChain Nodes running on different Docker containers.
 */
public class Bootstrap {

  static final String OUTPUT_PATH = "bootstrap.txt";

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

      for (Identifier id : idTable.keySet()) {
        System.out.println(id.toString() + " " + idTable.get(id));
      }

    } catch (IllegalStateException e) {
      System.err.println("could not initialize and run the Node network: " + e.getMessage());
      System.exit(1);
    }

  }

  private static void writeToOutput(ConcurrentMap<Identifier, String> idTable) {

    File file = new File(OUTPUT_PATH);
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(file));
      for (Identifier id : idTable.keySet()) {
        writer.write(id + ":" + idTable.get(id));
        writer.newLine();
      }
      writer.flush();
      writer.close();
    } catch (IOException e) {
      System.err.println("could open/close the file: " + e);
      System.exit(1);
    }

  }

}
