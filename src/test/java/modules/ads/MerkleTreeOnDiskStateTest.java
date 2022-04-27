package modules.ads;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import modules.ads.merkletree.MerkleTree;
import modules.ads.merkletree.MerkleTreeOnDiskState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import storage.mapdb.MerkleTreeStateMapDb;
import unittest.fixtures.EntityFixture;
import unittest.fixtures.MerkleTreeStateMapDbFixture;

/**
 * Tests for MerkleTreeOnDiskState.
 */
public class MerkleTreeOnDiskStateTest {
  private static final String TEMP_DIR = "tempdir";
  private static Path tempdir;
  MerkleTreeStateMapDb stateMapDb;
  MerkleTree merkleTree;

  /**
   * Set the tests up.
   */
  @BeforeEach
  void setUp() throws IOException {
    Path currentRelativePath = Paths.get("");
    tempdir = Files.createTempDirectory(currentRelativePath, TEMP_DIR);
    stateMapDb = MerkleTreeStateMapDbFixture.createMerkleTreeStateMapDb(tempdir);
    merkleTree = new MerkleTreeOnDiskState(stateMapDb);
    for (int i = 0; i < 5; i++) {
      merkleTree.put(new EntityFixture());
    }
  }

  /**
   * Clean up after the tests.
   */
  @AfterEach
  void tearDown() throws IOException {
    stateMapDb.closeDb();
    FileUtils.deleteDirectory(new File(tempdir.toString()));
  }

  /**
   * Test putting and verifying a random entity in a random an on disk merkle tree.
   */
  @Test
  public void testVerificationNoArg() {
    MerkleTreeTest.testVerification(null, merkleTree);
  }

  /**
   * Tests both putting and getting the same random entity gives same proof and putting
   * another entity gives different proofs.
   */
  @Test
  public void testPutGetSameProofNoArg() {
    MerkleTreeTest.testPutGetSameProof(null, merkleTree);
  }

  /**
   * Tests putting an existing entity does not change the proof with a random entity and an on disk merkle tree.
   */
  @Test
  public void testPutExistingEntityNoArg() throws IOException {
    MerkleTreeTest.testPutExistingEntity(null, merkleTree);
  }

  /**
   * Concurrently puts and gets entities and checks their proofs are correct (thread safety check).
   */
  @Test
  public void testConcurrentPutGetNoArg() throws IOException {
    stateMapDb.closeDb();
    FileUtils.deleteDirectory(new File(tempdir.toString()));
    Path currentRelativePath = Paths.get("");
    tempdir = Files.createTempDirectory(currentRelativePath, TEMP_DIR);
    stateMapDb = MerkleTreeStateMapDbFixture.createMerkleTreeStateMapDb(tempdir);
    merkleTree = new MerkleTreeOnDiskState(stateMapDb);
    MerkleTreeTest.testConcurrentPutGet(null, merkleTree);
  }

  /**
   * Tests getting an entity that does not exist in the merkle tree throws IllegalArgumentException
   * with a random entity and an on disk merkle tree.
   */
  @Test
  public void testGetNonExistingEntityNoArg() {
    MerkleTreeTest.testGetNonExistingEntity(null, merkleTree);
  }

  /**
   * Tests inserting null throws IllegalArgumentException with a random an on disk merkle tree.
   */
  @Test
  public void testNullInsertionNoArg() {
    MerkleTreeTest.testNullInsertion(merkleTree);
  }

  /**
   * Tests the proof verification fails when root is changed with random entities and an on disk merkle tree.
   */
  @Test
  public void testManipulatedRootNoArg() {
    MerkleTreeTest.testManipulatedRoot(null, merkleTree);
  }

  /**
   * Tests the proof verification fails when entity is changed with random entity and an on disk merkle tree.
   */
  @Test
  public void testManipulatedEntityNoArg() {
    MerkleTreeTest.testManipulatedEntity(null, merkleTree);
  }

  /**
   * Tests the proof verification fails when entity is changed with random entity and an on disk merkle tree.
   */
  @Test
  public void testManipulatedProofNoArg() {
    MerkleTreeTest.testManipulatedProof(null, merkleTree);
  }
}
