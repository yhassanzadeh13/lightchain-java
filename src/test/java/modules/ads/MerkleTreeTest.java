package modules.ads;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import model.Entity;
import model.crypto.Sha3256Hash;
import modules.ads.merkletree.MerkleTree;
import modules.ads.merkletree.Verifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unittest.fixtures.EntityFixture;
import unittest.fixtures.MerkleTreeFixture;

/**
 * Encapsulates tests for an authenticated and concurrent implementation of MerkleTree ADS.
 */
public class MerkleTreeTest {

  /**
   * A basic test for one concurrent put and get operations.
   */
  @Test
  public void testVerification() {
    MerkleTree merkleTree = MerkleTreeFixture.createMerkleTree(5);
    Entity entity = new EntityFixture();
    merkleTree.put(entity);
    AuthenticatedEntity authenticatedEntity = merkleTree.get(entity);
    Verifier verifier = new Verifier();
    Assertions.assertTrue(verifier.verify(authenticatedEntity));
  }

  /**
   * Tests both if putting and getting the same entity gives same proof
   * and putting another entity gives different proofs.
   */
  @Test
  public void testPutGetSameProof() {
    MerkleTree merkleTree = MerkleTreeFixture.createMerkleTree(5);
    Entity entity1 = new EntityFixture();
    AuthenticatedEntity authenticatedEntityPut = merkleTree.put(entity1);
    MembershipProof proofPut = authenticatedEntityPut.getMembershipProof();
    AuthenticatedEntity authenticatedEntityGet = merkleTree.get(entity1);
    MembershipProof proofGet = authenticatedEntityGet.getMembershipProof();
    Entity entity2 = new EntityFixture();
    AuthenticatedEntity authenticatedEntityPut2 = merkleTree.put(entity2);
    MembershipProof proofPut2 = authenticatedEntityPut2.getMembershipProof();
    Assertions.assertEquals(proofPut, proofGet);
    Assertions.assertNotEquals(proofPut, proofPut2);
  }

  /**
   * Tests if putting an existing entity does not change the proof.
   */
  @Test
  public void testPutExistingEntity() {
    MerkleTree merkleTree = MerkleTreeFixture.createMerkleTree(5);
    Entity entity = new EntityFixture();
    AuthenticatedEntity authenticatedEntityPut = merkleTree.put(entity);
    MembershipProof proofPut = authenticatedEntityPut.getMembershipProof();
    AuthenticatedEntity authenticatedEntityPutAgain = merkleTree.put(entity);
    MembershipProof proofPutAgain = authenticatedEntityPutAgain.getMembershipProof();
    Assertions.assertEquals(proofPut, proofPutAgain);
  }

  /**
   * Concurrently puts and gets entities and checks if their proofs are correct (thread safety check).
   */
  @Test
  public void testConcurrentPut() {
    int concurrencyDegree = 100;
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch countDownLatch = new CountDownLatch(concurrencyDegree);
    Thread[] merkleTreeThreads = new Thread[concurrencyDegree];
    MerkleTree merkleTree = MerkleTreeFixture.createMerkleTree(5);
    for (int i = 0; i < concurrencyDegree; i++) {
      merkleTreeThreads[i] = new Thread(() -> {
        Entity entity = new EntityFixture();
        try {
          merkleTree.put(entity);
          AuthenticatedEntity authenticatedEntity = merkleTree.get(entity);
          Verifier verifier = new Verifier();
          if (!verifier.verify(authenticatedEntity)) {
            threadError.getAndIncrement();
          }
          countDownLatch.countDown();
        } catch (NullPointerException e) {
          threadError.getAndIncrement();
        }
      });
    }
    for (Thread t : merkleTreeThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = countDownLatch.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    Assertions.assertEquals(0, threadError.get());
  }

  /**
   * Tests if getting an entity that does not exist in the merkle tree gives null.
   */
  @Test
  public void testGetNonExistingEntity() {
    MerkleTree merkleTree = MerkleTreeFixture.createMerkleTree(5);
    Entity entity = new EntityFixture();
    AuthenticatedEntity authenticatedEntity = merkleTree.get(entity);
    Assertions.assertNull(authenticatedEntity);
  }

  /**
   * Tests if inserting null gives null.
   */
  @Test
  public void testNullInsertion() {
    MerkleTree merkleTree = MerkleTreeFixture.createMerkleTree(5);
    AuthenticatedEntity authenticatedEntity = merkleTree.put(null);
    Assertions.assertNull(authenticatedEntity);
  }

  /**
   * Tests if the proof verifies when root is changed.
   */
  @Test
  public void testManipulatedRoot() {
    MerkleTree merkleTree = MerkleTreeFixture.createMerkleTree(5);
    Entity entity = new EntityFixture();
    AuthenticatedEntity authenticatedEntity = merkleTree.put(entity);
    MembershipProof proof = authenticatedEntity.getMembershipProof();
    proof.setRoot(new Sha3256Hash(new byte[32]));
    authenticatedEntity.setMembershipProof(proof);
    Verifier verifier = new Verifier();
    Assertions.assertFalse(verifier.verify(authenticatedEntity));
  }

  /**
   * Tests if the proof verifies when entity is changed.
   */
  @Test
  public void testManipulatedEntity() {
    MerkleTree merkleTree = MerkleTreeFixture.createMerkleTree(5);
    Entity entity = new EntityFixture();
    AuthenticatedEntity authenticatedEntity = merkleTree.put(entity);
    authenticatedEntity.setEntity(new EntityFixture());
    Verifier verifier = new Verifier();
    Assertions.assertFalse(verifier.verify(authenticatedEntity));
  }

  /**
   * Tests if the proof verifies when proof is changed.
   */
  @Test
  public void testManipulatedProof() {
    MerkleTree merkleTree = MerkleTreeFixture.createMerkleTree(5);
    Entity entity = new EntityFixture();
    AuthenticatedEntity authenticatedEntity = merkleTree.put(entity);
    MembershipProof proof = authenticatedEntity.getMembershipProof();
    ArrayList<Sha3256Hash> proofPath = proof.getPath();
    proofPath.add(new Sha3256Hash(new byte[32]));
    proof.setPath(proofPath);
    authenticatedEntity.setMembershipProof(proof);
    Verifier verifier = new Verifier();
    Assertions.assertFalse(verifier.verify(authenticatedEntity));
  }
}
