package modules.ads;

import model.Entity;
import model.crypto.Sha3256Hash;
import modules.ads.merkletree.MerkleTree;
import modules.ads.merkletree.Verifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unittest.fixtures.EntityFixture;
import unittest.fixtures.MerkleTreeFixture;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Encapsulates tests for an authenticated and concurrent implementation of SkipList ADS.
 */
public class MerkleTreeTest {
  // TODO: writing tests to cover
  // 1. When putting a unique entity into merkle tree, we can recover it.
  // 2. Proof of membership for putting and getting an entity is the same.
  // 3. Putting an already existing entity does not change its membership proof.
  // 4. Putting 100 distinct entities concurrently inserts all of them into merkle tree with correct membership proofs,
  //  and also, makes them all retrievable with correct membership proofs.
  // 5. Getting non-existing identifiers returns null.
  // 7. Putting null returns null.
  // 8. Tampering with root identifier of an authenticated entity fails its verification.
  // 9. Tampering with entity of an authenticated entity fails its verification.
  // 10. Tampering with proof of an authenticated entity fails its verification.

  @Test
  public void TestVerification() { // Test 1
    MerkleTree merkleTree = MerkleTreeFixture.createSkipList(5);
    Entity entity = new EntityFixture();
    merkleTree.put(entity);
    AuthenticatedEntity authenticatedEntity = merkleTree.get(entity);
    Verifier verifier = new Verifier();
    Assertions.assertTrue(verifier.verify(authenticatedEntity));
  }

  @Test
  public void TestPutGetSameProof() { // Test 2
    MerkleTree merkleTree = MerkleTreeFixture.createSkipList(5);
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

  @Test
  public void TestPutExistingEntity() { // Test 3
    MerkleTree merkleTree = MerkleTreeFixture.createSkipList(5);
    Entity entity = new EntityFixture();
    AuthenticatedEntity authenticatedEntityPut = merkleTree.put(entity);
    MembershipProof proofPut = authenticatedEntityPut.getMembershipProof();
    AuthenticatedEntity authenticatedEntityPutAgain = merkleTree.put(entity);
    MembershipProof proofPutAgain = authenticatedEntityPutAgain.getMembershipProof();
    Assertions.assertEquals(proofPut, proofPutAgain);
  }

  @Test
  public void TestConcurrentPut() { // Test 4
    int concurrencyDegree = 100;
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch countDownLatch = new CountDownLatch(concurrencyDegree);
    Thread[] merkleTreeThreads = new Thread[concurrencyDegree];
    MerkleTree merkleTree = MerkleTreeFixture.createSkipList(5);
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

  @Test
  public void TestGetNonExistingEntity() { // Test 5
    MerkleTree merkleTree = MerkleTreeFixture.createSkipList(5);
    Entity entity = new EntityFixture();
    AuthenticatedEntity authenticatedEntity = merkleTree.get(entity);
    Assertions.assertNull(authenticatedEntity);
  }

  @Test
  public void TestNullInsertion() { // Test 7
    MerkleTree merkleTree = MerkleTreeFixture.createSkipList(5);
    AuthenticatedEntity authenticatedEntity = merkleTree.put(null);
    Assertions.assertNull(authenticatedEntity);
  }

  @Test
  public void TestManipulatedRoot() { // Test 8
    MerkleTree merkleTree = MerkleTreeFixture.createSkipList(5);
    Entity entity = new EntityFixture();
    AuthenticatedEntity authenticatedEntity = merkleTree.put(entity);
    MembershipProof proof = authenticatedEntity.getMembershipProof();
    proof.setRoot(new Sha3256Hash(new byte[32]));
    authenticatedEntity.setMembershipProof(proof);
    Verifier verifier = new Verifier();
    Assertions.assertFalse(verifier.verify(authenticatedEntity));
  }

  @Test
  public void TestManipulatedEntity() { // Test 9
    MerkleTree merkleTree = MerkleTreeFixture.createSkipList(5);
    Entity entity = new EntityFixture();
    AuthenticatedEntity authenticatedEntity = merkleTree.put(entity);
    authenticatedEntity.setEntity(new EntityFixture());
    Verifier verifier = new Verifier();
    Assertions.assertFalse(verifier.verify(authenticatedEntity));
  }

  @Test
  public void TestManipulatedProof() { // Test 10
    MerkleTree merkleTree = MerkleTreeFixture.createSkipList(5);
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
