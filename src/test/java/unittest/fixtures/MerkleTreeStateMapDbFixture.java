package unittest.fixtures;

import java.io.IOException;
import java.nio.file.Path;

import storage.mapdb.MerkleTreeStateMapDb;

/**
 * Encapsulates utilities for the MerkleTreeStateMapDb.
 */
public class MerkleTreeStateMapDbFixture {
  private static final String TEMP_FILE = "tempfile.db";

  /**
   * Creates a new MerkleTreeStateMapDb instance.
   *
   * @param tempdir the temporary directory to use
   *
   * @return the new instance
   * @throws IOException if the path is not found
   */
  public static MerkleTreeStateMapDb createMerkleTreeStateMapDb(Path tempdir) throws IOException {
    return new MerkleTreeStateMapDb(tempdir.toAbsolutePath() + "/" + TEMP_FILE);
  }
}
