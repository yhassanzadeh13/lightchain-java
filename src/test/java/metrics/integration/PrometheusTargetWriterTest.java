package metrics.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static metrics.integration.PrometheusTargetWriter.writeTargetsToFile;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.Test;

/**
 * Test the PrometheusTargetWriter class.
 */
public class PrometheusTargetWriterTest {
  /**
   * Test that the list of targets is written to a file.
   *
   * @throws IOException if an error occurs while writing to the file
   */
  @Test
  public void testWriteTargetsToFile() throws IOException {
    List<String> targets = Arrays.asList("target1", "target2", "target3");
    String filePath = "test_file.json";
    writeTargetsToFile(targets, filePath);

    String expectedContent =
        "[\n" + "  {\n" + "    \"targets\": [\n" + "      \"target1\"\n" + "    ],\n" + "    \"labels\": {\n" + "      \"num\": \"0\"\n" + "    }\n"
            + "  },\n" + "  {\n" + "    \"targets\": [\n" + "      \"target2\"\n" + "    ],\n" + "    \"labels\": {\n" + "      \"num\": \"1\"\n"
            + "    }\n" + "  },\n" + "  {\n" + "    \"targets\": [\n" + "      \"target3\"\n" + "    ],\n" + "    \"labels\": {\n"
            + "      \"num\": \"2\"\n" + "    }\n" + "  }\n" + "]";

    // Read the file contents and compare to the expected JSON
    Path path = Paths.get(filePath);
    String fileContents = new String(Files.readAllBytes(path));
    assertEquals(expectedContent, fileContents);

    // Clean up the file after the test
    Files.deleteIfExists(path);
  }

  /**
   * Test that an IOException is thrown when the file writer fails.
   */
  @Test
  public void testWriteTargetsToFileIoException() {
    List<String> targets = Arrays.asList("target1", "target2", "target3");
    String filePath = "non_existent_directory/test_file.json";

    assertThrows(IOException.class, () -> writeTargetsToFile(targets, filePath));
  }
}
