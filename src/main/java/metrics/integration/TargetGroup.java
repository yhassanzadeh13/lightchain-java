package metrics.integration;

import java.util.Map;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TargetGroup {
  private final String NUM_LABEL = "num";
  private final List<TargetEntry> targets;

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
    return targets;
  }

  /**
   * TargetEntry is a nested data class that represents a single target entry for a Prometheus target group.
   * It contains the target address and an optional dictionary of labels.
   */
  public static class TargetEntry {
    private final List<String> targets;
    private final Map<String, String> labels;

    public TargetEntry(String targetAddress) {
      this.targets = new java.util.ArrayList<>();
      this.targets.add(targetAddress);
      this.labels = new java.util.HashMap<>();
    }

    public TargetEntry(String targetAddress, Map<String, String> labels) {
      this.targets = new java.util.ArrayList<>();
      this.targets.add(targetAddress);
      this.labels = new java.util.HashMap<>(labels);
    }

    public List<String> getTargets() {
      return targets;
    }

    public Map<String, String> getLabels() {
      return labels;
    }
  }
}
