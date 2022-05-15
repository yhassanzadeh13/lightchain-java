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
