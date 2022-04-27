package unittest.fixtures;

import storage.mapdb.MerkleTreeStateMapDb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MerkleTreeStateMapDbFixture {
  private static final String TEMP_FILE = "tempfile.db";

  public static MerkleTreeStateMapDb createMerkleTreeStateMapDb(Path tempdir) throws IOException {
    return new MerkleTreeStateMapDb(tempdir.toAbsolutePath() + "/" + TEMP_FILE);
  }
}
