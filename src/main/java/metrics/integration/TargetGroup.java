package metrics.integration;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * TargetGroup is a data class that contains the list of targets for a Prometheus target group.
 * It is designed to be JSON serialized.
 */
public class TargetGroup {
  private List<String> targets;

  public TargetGroup(List<String> targets) {
    this.targets = new java.util.ArrayList<>(targets);
  }

  public String toJsonString() {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    return gson.toJson(this);
  }

  public List<String> getTargets() {
    return targets;
  }
}
