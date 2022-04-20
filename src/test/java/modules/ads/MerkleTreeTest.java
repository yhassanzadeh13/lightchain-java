package modules.ads;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import model.Entity;
import model.crypto.Sha3256Hash;
import model.lightchain.Identifier;
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
    AuthenticatedEntity authenticatedEntity = merkleTree.get(entity.id());
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
    AuthenticatedEntity authenticatedEntityGet = merkleTree.get(entity1.id());
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
  public void testConcurrentPutGet() {
    int concurrencyDegree = 100;
    ArrayList<Entity> entities = new ArrayList<>();
    ArrayList<Identifier> ids = new ArrayList<>();
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch countDownLatchPut = new CountDownLatch(concurrencyDegree);
    CountDownLatch countDownLatchGet = new CountDownLatch(concurrencyDegree);
    Thread[] putThreads = new Thread[concurrencyDegree];
    Thread[] getThreads = new Thread[concurrencyDegree];
    MerkleTree merkleTree = MerkleTreeFixture.createMerkleTree(5);
    for (int i = 0; i < concurrencyDegree; i++) {
      Entity entity = new EntityFixture();
      entities.add(entity);
      ids.add(entity.id());
    }
    for (int i = 0; i < concurrencyDegree; i++) {
      Entity entity = entities.get(i);
      putThreads[i] = new Thread(() -> {
        try {
          merkleTree.put(entity);
          countDownLatchPut.countDown();
        } catch (NullPointerException e) {
          threadError.getAndIncrement();
        }
      });
    }
    for (Thread t : putThreads) {
      t.start();
    }
    try {
      boolean doneOneTimePut = countDownLatchPut.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTimePut);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    for (int i = 0; i < concurrencyDegree; i++) {
      Identifier id = ids.get(i);
      getThreads[i] = new Thread(() -> {
        try {
          AuthenticatedEntity authenticatedEntity = merkleTree.get(id);
          Verifier verifier = new Verifier();
          if (!verifier.verify(authenticatedEntity)) {
            threadError.getAndIncrement();
          }
          countDownLatchGet.countDown();
        } catch (NullPointerException e) {
          threadError.getAndIncrement();
        }
      });
    }
    for (Thread t : getThreads) {
      t.start();
    }
    try {
      boolean doneOneTimeGet = countDownLatchGet.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTimeGet);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    Assertions.assertEquals(0, threadError.get());
  }

  /**
   * Tests if getting an entity that does not exist in the merkle tree throws IllegalArgumentException.
   */
  @Test
  public void testGetNonExistingEntity() {
    MerkleTree merkleTree = MerkleTreeFixture.createMerkleTree(5);
    Entity entity = new EntityFixture();
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      merkleTree.get(entity.id());
    });
  }

  /**
   * Tests if inserting null throws IllegalArgumentException.
   */
  @Test
  public void testNullInsertion() {
    MerkleTree merkleTree = MerkleTreeFixture.createMerkleTree(5);
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      merkleTree.put(null);
    });
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
    proofPath.set(0, new Sha3256Hash(new byte[32]));
    proof.setPath(proofPath);
    authenticatedEntity.setMembershipProof(proof);
    Verifier verifier = new Verifier();
    Assertions.assertFalse(verifier.verify(authenticatedEntity));
  }
}
