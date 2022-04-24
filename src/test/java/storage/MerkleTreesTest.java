package storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import model.Entity;
import modules.ads.AuthenticatedEntity;
import modules.ads.merkletree.MerkleTree;
import modules.ads.merkletree.MerkleTreeAuthenticatedEntityVerifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import storage.mapdb.MerkleTreeMapDb;
import unittest.fixtures.EntityFixture;
import unittest.fixtures.MerkleTreeFixture;

public class MerkleTreesTest {
  private static final String TEMP_DIR = "tempdir";
  private static final String TEMP_FILE = "tempfile.db";
  private Path tempdir;
  private ArrayList<MerkleTree> allTrees;
  private MerkleTreeMapDb db;

  /**
   * Sets up the tests.
   *
   * @throws IOException if the temp directory cannot be created
   */
  @BeforeEach
  void setUp() throws IOException {
    Path currentRelativePath = Paths.get("");
    tempdir = Files.createTempDirectory(currentRelativePath, TEMP_DIR);
    db = new MerkleTreeMapDb(tempdir.toAbsolutePath() + "/" + TEMP_FILE);
    allTrees = MerkleTreeFixture.newMerkleTrees(10, 5);
  }

  @Test
  public void testVerification() throws IOException {
    for (MerkleTree tree : allTrees) {
      Assertions.assertTrue(db.add(tree));
    }
    for (MerkleTree tree : allTrees) {
      Assertions.assertTrue(db.has(tree));
    }
    ArrayList<MerkleTree> all = db.all();
    Assertions.assertEquals(all.size(), 10);
    for (MerkleTree merkleTree : all) {
      Assertions.assertEquals(merkleTree.size(), 5); // fixture sanity check.
      Entity entity = new EntityFixture();
      merkleTree.put(entity);
      Assertions.assertEquals(merkleTree.size(), 6);
      AuthenticatedEntity authenticatedEntity = merkleTree.get(entity.id());
      MerkleTreeAuthenticatedEntityVerifier verifier = new MerkleTreeAuthenticatedEntityVerifier();
      Assertions.assertTrue(verifier.verify(authenticatedEntity));
    }
    db.closeDb();
    FileUtils.deleteDirectory(new File(tempdir.toString()));
  }

}
