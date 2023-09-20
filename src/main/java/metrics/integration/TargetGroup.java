package metrics.integration;

import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * TargetGroup is a data class that represents a Prometheus target group.
 * It contains a list of target entries.
 * Prometheus target groups are described here: https://prometheus.io/docs/prometheus/latest/configuration/configuration/#target_group
 * They are used to dynamically update the list of targets that Prometheus scrapes.
 * In this implementation, the target group is written to a file that is read by the Prometheus file_sd_config.
 */
public class TargetGroup {
  private static final String NUM_LABEL = "num";
  private final List<TargetEntry> targets;

  /**
   * Create a new TargetGroup from a list of target addresses.
   *
   * @param targetAddresses list of target addresses, in the format host:port. In LightChain we expect format to be like node1:8081.
   */
  public TargetGroup(List<String> targetAddresses) {
    this.targets = new java.util.ArrayList<>();
    for (int i = 0; i < targetAddresses.size(); i++) {
      String address = targetAddresses.get(i);
      Map<String, String> labels = Map.of(NUM_LABEL, String.valueOf(i));
      this.targets.add(new TargetEntry(address, labels));
    }
  }

  public String toJsonString() {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    return gson.toJson(this.targets);
  }

  public List<TargetEntry> getTargets() {
    return List.copyOf(targets);
  }

  /**
   * TargetEntry is a nested data class that represents a single target entry for a Prometheus target group.
   * It contains the target address and an optional dictionary of labels.
   */
  public static class TargetEntry {
    private final List<String> targets;
    private final Map<String, String> labels;

    /**
     * Create a new TargetEntry from a target address and a dictionary of labels.
     *
     * @param targetAddress target address, in the format host:port. In LightChain we expect format to be like node1:8081.
     * @param labels dictionary of labels.
     *               In LightChain we expect the dictionary to contain a single key-value pair, with key "num" and value "0", "1", etc.
     */
    public TargetEntry(String targetAddress, Map<String, String> labels) {
      this.targets = new java.util.ArrayList<>();
      this.targets.add(targetAddress);
      this.labels = new java.util.HashMap<>(labels);
    }

    public List<String> getTargets() {
      return List.copyOf(targets);
    }

    public Map<String, String> getLabels() {
      return Map.copyOf(labels);
    }
  }
}
