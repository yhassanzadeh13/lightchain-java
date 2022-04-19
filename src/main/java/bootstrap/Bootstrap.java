package bootstrap;

import java.io.*;
import java.util.Map;
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

      for (Map.Entry<Identifier, String> id : idTable.entrySet()) {
        System.out.println(id.getKey().toString() + " " + idTable.get(id.getKey()));
      }

    } catch (IllegalStateException e) {
      System.err.println("could not initialize and run the Node network: " + e.getMessage());
      System.exit(1);
    }

  }

  private static void writeToOutput(ConcurrentMap<Identifier, String> idTable) {

    File file = new File(OUTPUT_PATH);
    try {
      FileOutputStream fileStream = new FileOutputStream(file);
      Writer writer = new OutputStreamWriter(fileStream, "UTF-8");
      for (Map.Entry<Identifier, String> id : idTable.entrySet()) {
        writer.write(id.getKey() + ":" + idTable.get(id.getKey()) + "\n");
      }
      writer.flush();
      writer.close();
    } catch (IOException e) {
      System.err.println("could open/close the file: " + e);
      System.exit(1);
    }

  }

}
