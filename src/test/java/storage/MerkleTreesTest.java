package storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import model.Entity;
import modules.ads.AuthenticatedEntity;
import modules.ads.MerkleTreeTest;
import modules.ads.merkletree.MerkleTree;
import modules.ads.merkletree.MerkleTreeAuthenticatedEntityVerifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import storage.mapdb.EntityMapDb;
import storage.mapdb.MerkleNodeMapDb;
import storage.mapdb.MerkleTreeMapDb;
import unittest.fixtures.EntityFixture;
import unittest.fixtures.MerkleTreeFixture;

public class MerkleTreesTest {
  private static final String TEMP_DIR_1 = "tempdir1";
  private static final String TEMP_FILE_1 = "tempfile1.db";
  private static final String TEMP_DIR_2 = "tempdir2";
  private static final String TEMP_FILE_2 = "tempfile2.db";
  private Path tempdirNode;
  private Path tempdirEntity;
  private ArrayList<MerkleTree> allTrees;
  private MerkleNodeMapDb dbNode;
  private EntityMapDb dbEntity;
  private MerkleTreeMapDb dbMerkleTree;

  /**
   * Sets up the tests.
   *
   * @throws IOException if the temp directory cannot be created
   */
  @BeforeEach
  void setUp() throws IOException {
    Path currentRelativePath = Paths.get("");
    tempdirNode = Files.createTempDirectory(currentRelativePath, TEMP_DIR_1);
    tempdirEntity = Files.createTempDirectory(currentRelativePath, TEMP_DIR_2);
    dbNode = new MerkleNodeMapDb(tempdirEntity.toAbsolutePath() + "/" + TEMP_FILE_1);
    dbEntity = new EntityMapDb(tempdirEntity.toAbsolutePath() + "/" + TEMP_FILE_2);
    dbMerkleTree = new MerkleTreeMapDb(dbNode, dbEntity);
  }

  @AfterEach
  void tearDown() throws IOException {
    dbNode.closeDb();
    dbEntity.closeDb();
    dbMerkleTree.closeDb();
    FileUtils.deleteDirectory(tempdirNode.toFile());
    FileUtils.deleteDirectory(tempdirEntity.toFile());
  }
  @Test
  public void testVerification() {
    for (int i = 0; i < 5; i++) {
      Entity entity = new EntityFixture();
      dbMerkleTree.put(entity);
    }
    MerkleTreeTest.testVerification(null, dbMerkleTree);
  }

  @Test
  public void testPutGetSameProof() {
    for (int i = 0; i < 5; i++) {
      Entity entity = new EntityFixture();
      dbMerkleTree.put(entity);
    }
    MerkleTreeTest.testPutGetSameProof(null, dbMerkleTree);
  }

  @Test
  public void testPutExistingEntity() {
    for (int i = 0; i < 5; i++) {
      Entity entity = new EntityFixture();
      dbMerkleTree.put(entity);
    }
    MerkleTreeTest.testPutExistingEntity(null, dbMerkleTree);
  }

  @Test
  public void testConcurrentPutGet() {
    MerkleTreeTest.testConcurrentPutGet(null, dbMerkleTree);
  }

  @Test
  public void testGetNonExistingEntity() {
    for (int i = 0; i < 5; i++) {
      Entity entity = new EntityFixture();
      dbMerkleTree.put(entity);
    }
    MerkleTreeTest.testGetNonExistingEntity(null, dbMerkleTree);
  }

  @Test
  public void testNullInsertion() {
    for (int i = 0; i < 5; i++) {
      Entity entity = new EntityFixture();
      dbMerkleTree.put(entity);
    }
    MerkleTreeTest.testNullInsertion(dbMerkleTree);
  }

  @Test
  public void testManipulatedRoot() {
    for (int i = 0; i < 5; i++) {
      Entity entity = new EntityFixture();
      dbMerkleTree.put(entity);
    }
    MerkleTreeTest.testManipulatedRoot(null, dbMerkleTree);
  }

  @Test
  public void testManipulatedEntity() {
    for (int i = 0; i < 5; i++) {
      Entity entity = new EntityFixture();
      dbMerkleTree.put(entity);
    }
    MerkleTreeTest.testManipulatedEntity(null, dbMerkleTree);
  }

  @Test
  public void testManipulatedProof() {
    for (int i = 0; i < 5; i++) {
      Entity entity = new EntityFixture();
      dbMerkleTree.put(entity);
    }
    MerkleTreeTest.testManipulatedProof(null, dbMerkleTree);
  }
}
