package metrics.integration;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for TargetGroup class.
 */
public class TargetGroupTest {

  @Test
  public void testConstructor() {
    List<String> targetAddresses = List.of("example1.com:9090", "example2.com:9090");
    TargetGroup targetGroup = new TargetGroup(targetAddresses);
    assertNotNull(targetGroup);

    List<TargetGroup.TargetEntry> targetEntries = targetGroup.getTargets();
    assertEquals(targetAddresses.size(), targetEntries.size());

    for (int i = 0; i < targetAddresses.size(); i++) {
      assertEquals(List.of(targetAddresses.get(i)), targetEntries.get(i).getTargets());
      assertEquals(Map.of("num", String.valueOf(i)), targetEntries.get(i).getLabels());
    }
  }

  @Test
  public void testToJsonString() {
    List<String> targetAddresses = List.of("example1.com:9090", "example2.com:9090");
    TargetGroup targetGroup = new TargetGroup(targetAddresses);
    String expectedJson =
        "[\n" + "  {\n" + "    \"targets\": [\n" + "      \"example1.com:9090\"\n" + "    ],\n" + "    \"labels\": {\n" + "      \"num\": \"0\"\n"
            + "    }\n" + "  },\n" + "  {\n" + "    \"targets\": [\n" + "      \"example2.com:9090\"\n" + "    ],\n" + "    \"labels\": {\n"
            + "      \"num\": \"1\"\n" + "    }\n" + "  }\n" + "]";
    String actualJson = targetGroup.toJsonString();
    assertEquals(expectedJson, actualJson);
  }
}
