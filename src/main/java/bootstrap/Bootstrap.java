package bootstrap;

import integration.localnet.LocalTestNet;
import model.lightchain.Identifier;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ConcurrentMap;

public class Bootstrap {

  final static String OUTPUT_PATH = "bootstrap.txt";

  public static void main(String[] args) {

    int nodeCount = 100;
    ConcurrentMap<Identifier, String> idTable;

    LocalTestNet testNet = new LocalTestNet(nodeCount);

    try {
      idTable = testNet.createNodeContainers();

      writeToOutput(idTable);

      for (Identifier id : idTable.keySet()) {
        System.out.println(id.toString() + " " + idTable.get(id));
      }

    } catch (IllegalStateException e) {
      System.err.println("could not initialize and run node net: " + e.getMessage());
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
      e.printStackTrace();
    }

  }

}
