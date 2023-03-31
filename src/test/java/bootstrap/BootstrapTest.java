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
import org.junit.jupiter.api.Test;
import unittest.fixtures.IdentifierFixture;

/**
 * Tests for bootstrap class {@link Bootstrap}.
 */
public class BootstrapTest {
  /**
   * testMakeBootstrap tests the makeBootstrap method of the bootstrap class.
   * It creates a bootstrap object with 3 nodes and checks if the docker names, identifiers, id table and metrics table are correct.
   */
  @Test
  public void testMakeBootstrap() {
    // creates a bootstrap object with 3 nodes.
    Bootstrap bootstrap = new Bootstrap((short) 3);
    bootstrap.makeBootstrap();

    List<String> dockerNames = bootstrap.getDockerNames();
    assertEquals(3, dockerNames.size());

    List<Identifier> identifiers = bootstrap.getIdentifiers();
    assertEquals(3, identifiers.size());

    Map<Identifier, String> idTable = bootstrap.getIdTable();
    assertEquals(3, idTable.size());

    Map<Identifier, String> metricsTable = bootstrap.getMetricsTable();
    assertEquals(3, metricsTable.size());

    // Check if the id table and metrics table contain the correct values. 
    for (int i = 0; i < 3; i++) {
      String dockerName = dockerNames.get(i);
      Identifier id = identifiers.get(i);
      // idTable keeps a map of the node's identifier and the node's fully qualified address (in LightChain protocol).
      String fullyQualifiedAddress = idTable.get(id);
      // metrics table keeps a map of the node's identifier and the node's metrics server address (the address that the metrics server is exposing
      // the metrics to).
      String metricsServerAddress = metricsTable.get(id);

      // dockerName + ":" + Bootstrap.bootstrapPortNumber should be equal the fully qualified address of the node in the docker network.
      assertEquals(dockerName + ":" + Bootstrap.bootstrapPortNumber, fullyQualifiedAddress);
      // dockerName + ":" + MetricServer.SERVER_PORT should be equal the metrics address of the node in the docker network.
      assertEquals(dockerName + ":" + MetricServer.SERVER_PORT, metricsServerAddress);
    }
  }

  /**
   * testWriteOnFile tests the writeOnFile method of the bootstrap class.
   * It creates a bootstrap object with 3 nodes and checks if the bootstrap file is created and if the content of the file is correct.
   */
  @Test
  public void testWriteOnFile() throws IOException {
    // creates a bootstrap object with 3 nodes.
    Bootstrap bootstrap = new Bootstrap((short) 3);
    bootstrap.makeBootstrap();

    // loads the bootstrap file and checks if it exists.
    File file = new File(bootstrap.getBootstrapFileName());
    assertTrue(file.exists());

    // checks if the content of the size of the bootstrap file is correct (3 lines).
    List<String> lines = Files.readAllLines(Path.of(bootstrap.getBootstrapFileName()));
    assertEquals(3, lines.size());

    // checks if the content of the bootstrap file is correct.
    Map<Identifier, String> idTable = bootstrap.getIdTable();
    for (String line : lines) {
      // each line should be in the format of "identifier:fullyQualifiedAddress"
      String[] split = line.split(":");
      Identifier id = new Identifier(split[0]);
      String fullyQualifiedAddress = idTable.get(id);
      assertEquals(line, id + ":" + fullyQualifiedAddress);
    }

    file.deleteOnExit();
  }

  /**
   * testPrint tests the print method of the bootstrap class,
   * it creates a bootstrap object with 3 nodes and checks if the console output is correct.
   */
  @Test
  public void testPrint() {
    // creates a bootstrap object with 3 nodes.
    Bootstrap bootstrap = new Bootstrap((short) 3);
    bootstrap.makeBootstrap();

    // redirects console output to check if it matches expected output
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outputStream));

    bootstrap.print();

    String output = outputStream.toString();
    // checks the header of the console output.
    assertTrue(output.contains("bootstrap file created with the following content:"));
    // checks the name of the bootstrap file.
    assertTrue(output.contains("bootstrap file written to " + bootstrap.getBootstrapFileName()));

    // each subsequent line should be in the format of "identifier fullyQualifiedAddress"
    Map<Identifier, String> idTable = bootstrap.getIdTable();
    for (Identifier id : idTable.keySet()) {
      assertTrue(output.contains(id.toString() + " " + idTable.get(id)));
    }
  }

  /**
   * testReadFile tests the readFile method of the bootstrap class.
   * It creates a bootstrap file with 3 lines and checks if the content of the file is correct.
   */
  @Test
  public void testReadFile() throws IOException {
    // creates a temporary file with test data.
    Path tempFile = Files.createTempFile("bootstrap", ".txt");
    Identifier id1 = IdentifierFixture.newIdentifier();
    Identifier id2 = IdentifierFixture.newIdentifier();
    Identifier id3 = IdentifierFixture.newIdentifier();
    Files.write(tempFile, List.of(
        String.format("%s:%s:%d", id1, "node1", 1234),
        String.format("%s:%s:%d", id2, "node2", 5678),
        String.format("%s:%s:%d", id3, "node3", 9012)));

    // reads the file into a map of identifier and fully qualified address.
    Map<Identifier, String> map = Bootstrap.readFile(tempFile.toString());

    // checks if the size of the map is correct.
    assertEquals(3, map.size());

    // each map entry should represent a map of identifier and fully qualified address.
    assertEquals("node1:1234", map.get(id1));
    assertEquals("node2:5678", map.get(id2));
    assertEquals("node3:9012", map.get(id3));

    Files.deleteIfExists(tempFile);
  }

  // testRoundTrip tests the round trip of the bootstrap class. It creates a bootstrap object, writes it to a file and reads it back.
  // It then checks if the original and new bootstraps are equivalent.
  @Test
  public void testRoundTrip() throws IOException {
    // create the original bootstrap.
    Bootstrap originalBootstrap = new Bootstrap((short) 3);
    originalBootstrap.build();

    // writes the bootstrap to the file.
    originalBootstrap.writeOnFile();

    // read the bootstrap from the file.
    Map<Identifier, String> idTable = Bootstrap.readFile(originalBootstrap.getBootstrapFileName());

    // verifies that the original and new bootstraps are equivalent, the id table should be the same mapping of identifier to fully qualified address.
    assertEquals(originalBootstrap.getIdTable(), idTable);
  }
}
