package modules.ads;

import org.junit.jupiter.api.Test;

/**
 * Tests for MerkleTreeInMemoryState.
 */
public class MerkleTreeInMemoryStateTest {

  /**
   * Test putting and verifying a random entity in a random merkle tree.
   */
  @Test
  public void testVerificationNoArg() {
    MerkleTreeTest.testVerification(null, null);
  }

  /**
   * Tests both putting and getting the same random entity gives same proof and putting
   * another entity gives different proofs.
   */
  @Test
  public void testPutGetSameProofNoArg() {
    MerkleTreeTest.testPutGetSameProof(null, null);
  }

  /**
   * Tests putting an existing entity does not change the proof with a random entity and merkle tree.
   */
  @Test
  public void testPutExistingEntityNoArg() {
    MerkleTreeTest.testPutExistingEntity(null, null);
  }

  /**
   * Concurrently puts and gets entities and checks their proofs are correct (thread safety check).
   */
  @Test
  public void testConcurrentPutGetNoArg() {
    MerkleTreeTest.testConcurrentPutGet(null, null);
  }

  /**
   * Tests getting an entity that does not exist in the merkle tree throws IllegalArgumentException
   * with a random entity and merkle tree.
   */
  @Test
  public void testGetNonExistingEntityNoArg() {
    MerkleTreeTest.testGetNonExistingEntity(null, null);
  }

  /**
   * Tests inserting null throws IllegalArgumentException with a random merkle tree.
   */
  @Test
  public void testNullInsertionNoArg() {
    MerkleTreeTest.testNullInsertion(null);
  }

  /**
   * Tests the proof verification fails when root is changed with random entities and merkle tree.
   */
  @Test
  public void testManipulatedRootNoArg() {
    MerkleTreeTest.testManipulatedRoot(null, null);
  }

  /**
   * Tests the proof verification fails when entity is changed with random entity and merkle tree.
   */
  @Test
  public void testManipulatedEntityNoArg() {
    MerkleTreeTest.testManipulatedEntity(null, null);
  }

  /**
   * Tests the proof verification fails when entity is changed with random entity and merkle tree.
   */
  @Test
  public void testManipulatedProofNoArg() {
    MerkleTreeTest.testManipulatedProof(null, null);
  }
}
