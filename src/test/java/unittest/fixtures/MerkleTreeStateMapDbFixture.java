package unittest.fixtures;

import java.io.IOException;
import java.nio.file.Path;

import storage.mapdb.MerkleTreeStateMapDb;

public class MerkleTreeStateMapDbFixture {
  private static final String TEMP_FILE = "tempfile.db";

  public static MerkleTreeStateMapDb createMerkleTreeStateMapDb(Path tempdir) throws IOException {
    return new MerkleTreeStateMapDb(tempdir.toAbsolutePath() + "/" + TEMP_FILE);
  }
}
