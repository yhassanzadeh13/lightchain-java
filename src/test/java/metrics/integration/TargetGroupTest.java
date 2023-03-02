package metrics.integration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class TargetGroupTest {

  @Test
  public void testConstructor() {
    List<String> targets = List.of("example1.com:9090", "example2.com:9090");
    TargetGroup targetGroup = new TargetGroup(targets);
    assertNotNull(targetGroup);
    assertEquals(targets, targetGroup.getTargets());
  }

  @Test
  public void testToJsonString() {
    List<String> targets = List.of("example1.com:9090", "example2.com:9090");
    TargetGroup targetGroup = new TargetGroup(targets);
    String expectedJson = "{\n  \"targets\": [\n    \"example1.com:9090\",\n    \"example2.com:9090\"\n  ]\n}";
    String actualJson = targetGroup.toJsonString();
    assertEquals(expectedJson, actualJson);
  }
}
