package bootstrap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import metrics.collectors.MetricServer;
import model.lightchain.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for bootstrap class {@link Bootstrap}.
 */
public class BootstrapTest {

  private Bootstrap bootstrap;

  @BeforeEach
  public void setup() {
    this.bootstrap = new Bootstrap((short) 3);
  }

  @Test
  public void testMakeBootstrap() {
    this.bootstrap.makeBootstrap();

    List<String> dockerNames = this.bootstrap.getDockerNames();
    assertEquals(3, dockerNames.size());

    List<Identifier> identifiers = this.bootstrap.getIdentifiers();
    assertEquals(3, identifiers.size());

    Map<Identifier, String> idTable = this.bootstrap.getIdTable();
    assertEquals(3, idTable.size());

    Map<Identifier, String> metricsTable = this.bootstrap.getMetricsTable();
    assertEquals(3, metricsTable.size());

    for (int i = 0; i < 3; i++) {
      String dockerName = dockerNames.get(i);
      Identifier id = identifiers.get(i);
      String idTableValue = idTable.get(id);
      String metricsTableValue = metricsTable.get(id);

      assertEquals(dockerName + ":" + Bootstrap.bootstrapPortNumber, idTableValue);
      assertEquals(dockerName + ":" + MetricServer.SERVER_PORT, metricsTableValue);
    }
  }

  @Test
  public void testWriteOnFile() throws IOException {
    this.bootstrap.makeBootstrap();
    this.bootstrap.writeOnFile();

    File file = new File(this.bootstrap.getBootstrapFileName());
    assertTrue(file.exists());

    List<String> lines = Files.readAllLines(Path.of(this.bootstrap.getBootstrapFileName()));
    assertEquals(3, lines.size());

    Map<Identifier, String> idTable = this.bootstrap.getIdTable();
    for (String line : lines) {
      String[] split = line.split(":");
      Identifier id = new Identifier(split[0]);
      String idTableValue = idTable.get(id);
      assertEquals(line, id + ":" + idTableValue);
    }

    file.deleteOnExit();
  }

  @Test
  public void testPrint() {
    this.bootstrap.makeBootstrap();

    // Redirect console output to check if it matches expected output
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outputStream));

    this.bootstrap.print();

    String output = outputStream.toString();
    assertTrue(output.contains("bootstrap file created with the following content:"));
    assertTrue(output.contains("bootstrap file written to " + this.bootstrap.getBootstrapFileName()));

    Map<Identifier, String> idTable = this.bootstrap.getIdTable();
    for (Identifier id : idTable.keySet()) {
      assertTrue(output.contains(id.toString() + " " + idTable.get(id)));
    }
  }

  @Test
  public void testReadFile() throws IOException {
    // Create a temporary file with test data
    Path tempFile = Files.createTempFile("bootstrap", ".txt");
    Files.write(tempFile, List.of(
        "id1:node1:1234",
        "id2:node2:5678",
        "id3:node3:9012"
                                 ));

    Map<Identifier, String> map = Bootstrap.readFile(tempFile.toString());

    assertEquals(3, map.size());
    assertEquals("node1:1234", map.get(new Identifier("id1")));
    assertEquals("node2:5678", map.get(new Identifier("id2")));
    assertEquals("node3:9012", map.get(new Identifier("id3")));

    Files.deleteIfExists(tempFile);
  }

  @Test
  public void testRoundTrip() throws IOException {
    // Create the original bootstrap
    Bootstrap originalBootstrap = new Bootstrap((short) 3);
    originalBootstrap.build();

    // Write the bootstrap to the file
    originalBootstrap.writeOnFile();

    // Read the bootstrap from the file
    Map<Identifier, String> idTable = Bootstrap.readFile(originalBootstrap.getBootstrapFileName());

    // Verify that the original and new bootstraps are equivalent
    assertEquals(originalBootstrap.getIdTable(), idTable);
  }
}
