package metrics.integration;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * PrometheusTargetWriter is a utility class that writes a list of targets to a file.
 * The file is in the format expected by Prometheus.
 */
public class PrometheusTargetWriter {
  /**
   * Write the list of targets to a file.
   *
   * @param targets list of targets
   * @throws IOException if an error occurs while writing to the file
   */
  public static void writeTargetsToFile(List<String> targets, String filePath) throws IOException {
    // Define the structure of the JSON file
    String targetGroupsJson = new TargetGroup(targets).toJsonString();

    // Write the JSON to a file
    try (FileWriter fileWriter = new FileWriter(filePath)) {
      fileWriter.write(targetGroupsJson);
    } catch (IOException e) {
      throw new IOException("failed to write targets to file", e);
    }
  }
}
