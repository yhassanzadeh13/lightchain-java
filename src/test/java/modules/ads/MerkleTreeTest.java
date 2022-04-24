package modules.ads;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import model.Entity;
import model.crypto.Sha3256Hash;
import model.lightchain.Identifier;
import modules.ads.merkletree.AuthenticatedEntityVerifier;
import modules.ads.merkletree.MerkleTree;
import modules.ads.merkletree.MerkleTreeAuthenticatedEntity;
import modules.ads.merkletree.Proof;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unittest.fixtures.EntityFixture;
import unittest.fixtures.MerkleTreeFixture;
import unittest.fixtures.Sha3256HashFixture;

/**
 * Encapsulates tests for an authenticated and concurrent implementation of MerkleTree ADS.
 */
public class MerkleTreeTest {

  /**
   * A basic test for one sequential put and get operations.
   */
  @Test
  public void testVerification() {
    MerkleTree merkleTree = MerkleTreeFixture.createMerkleTree(5);
    Assertions.assertEquals(merkleTree.size(), 5); // fixture sanity check.

    Entity entity = new EntityFixture();
    merkleTree.put(entity);
    Assertions.assertEquals(merkleTree.size(), 6);

    AuthenticatedEntity authenticatedEntity = merkleTree.get(entity.id());
    AuthenticatedEntityVerifier verifier = new AuthenticatedEntityVerifier();
    Assertions.assertTrue(verifier.verify(authenticatedEntity));
  }

  /**
   * Tests both putting and getting the same entity gives same proof
   * and putting another entity gives different proofs.
   */
  @Test
  public void testPutGetSameProof() {
    MerkleTree merkleTree = MerkleTreeFixture.createMerkleTree(5);
    Assertions.assertEquals(merkleTree.size(), 5); // fixture sanity check.
    Entity e1 = new EntityFixture();

    // putting e1
    AuthenticatedEntity authenticatedEntityPut = merkleTree.put(e1);
    MembershipProof proofPutE1 = authenticatedEntityPut.getMembershipProof();
    Assertions.assertEquals(merkleTree.size(), 6);

    // getting e1
    AuthenticatedEntity authE1Get = merkleTree.get(e1.id());
    MembershipProof proofGetE1 = authE1Get.getMembershipProof();

    // putting e2
    Entity e2 = new EntityFixture();
    AuthenticatedEntity authE2Put = merkleTree.put(e2);
    Assertions.assertEquals(merkleTree.size(), 7);

    // getting e2
    MembershipProof proofPutE2 = authE2Put.getMembershipProof();

    // proofs for putting and getting e1 should be the same.
    Assertions.assertEquals(proofPutE1, proofGetE1);

    // proofs for putting e1 and e2 must be different.
    Assertions.assertNotEquals(proofPutE1, proofPutE2);
  }

  /**
   * Tests putting an existing entity does not change the proof.
   */
  @Test
  public void testPutExistingEntity() {
    MerkleTree merkleTree = MerkleTreeFixture.createMerkleTree(5);
    Assertions.assertEquals(merkleTree.size(), 5); // fixture sanity check.
    Entity entity = new EntityFixture();

    // first time put
    AuthenticatedEntity authenticatedEntityPut = merkleTree.put(entity);
    MembershipProof proofPut = authenticatedEntityPut.getMembershipProof();
    Assertions.assertEquals(merkleTree.size(), 6);

    // second attempt
    AuthenticatedEntity authenticatedEntityPutAgain = merkleTree.put(entity);
    MembershipProof proofPutAgain = authenticatedEntityPutAgain.getMembershipProof();

    // proofs must be equal.
    Assertions.assertEquals(proofPut, proofPutAgain);
    Assertions.assertEquals(merkleTree.size(), 6); // duplicate entity should not change the size.
  }

  /**
   * Concurrently puts and gets entities and checks their proofs are correct (thread safety check).
   */
  @Test
  public void testConcurrentPutGet() {
    int concurrencyDegree = 100;
    ArrayList<Entity> entities = new ArrayList<>();
    ArrayList<Identifier> ids = new ArrayList<>();

    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch putDone = new CountDownLatch(concurrencyDegree);
    CountDownLatch getDone = new CountDownLatch(concurrencyDegree);

    Thread[] putThreads = new Thread[concurrencyDegree];
    Thread[] getThreads = new Thread[concurrencyDegree];

    MerkleTree merkleTree = MerkleTreeFixture.createMerkleTree(0);
    Assertions.assertEquals(merkleTree.size(), 0); // fixture sanity check.

    for (int i = 0; i < concurrencyDegree; i++) {
      Entity entity = new EntityFixture();
      entities.add(entity);
      ids.add(entity.id());
    }

    // put
    for (int i = 0; i < concurrencyDegree; i++) {
      Entity entity = entities.get(i);
      putThreads[i] = new Thread(() -> {
        try {
          merkleTree.put(entity);
          putDone.countDown();
        } catch (Exception e) {
          threadError.getAndIncrement();
        }
      });
    }
    for (Thread t : putThreads) {
      t.start();
    }
    try {
      boolean doneOneTimePut = putDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTimePut);
    } catch (InterruptedException e) {
      Assertions.fail();
    }

    // get
    for (int i = 0; i < concurrencyDegree; i++) {
      Identifier id = ids.get(i);
      getThreads[i] = new Thread(() -> {
        try {
          AuthenticatedEntity authenticatedEntity = merkleTree.get(id);
          AuthenticatedEntityVerifier verifier = new AuthenticatedEntityVerifier();
          if (!verifier.verify(authenticatedEntity)) {
            threadError.getAndIncrement();
          }
          getDone.countDown();
        } catch (Exception e) {
          threadError.getAndIncrement();
        }
      });
    }
    for (Thread t : getThreads) {
      t.start();
    }
    try {
      boolean doneOneTimeGet = getDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTimeGet);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    Assertions.assertEquals(0, threadError.get());
    Assertions.assertEquals(concurrencyDegree, merkleTree.size());
  }

  /**
   * Tests getting an entity that does not exist in the merkle tree throws IllegalArgumentException.
   */
  @Test
  public void testGetNonExistingEntity() {
    MerkleTree merkleTree = MerkleTreeFixture.createMerkleTree(5);
    Assertions.assertEquals(merkleTree.size(), 5); // fixture sanity check.
    Entity entity = new EntityFixture();

    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      merkleTree.get(entity.id());
    });
  }

  /**
   * Tests inserting null throws IllegalArgumentException.
   */
  @Test
  public void testNullInsertion() {
    MerkleTree merkleTree = MerkleTreeFixture.createMerkleTree(5);
    Assertions.assertEquals(merkleTree.size(), 5); // fixture sanity check.
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      merkleTree.put(null);
    });
  }

  /**
   * Tests the proof verification fails when root is changed.
   */
  @Test
  public void testManipulatedRoot() {
    MerkleTree merkleTree = MerkleTreeFixture.createMerkleTree(5);
    Assertions.assertEquals(merkleTree.size(), 5); // fixture sanity check.
    Entity entity = new EntityFixture();
    AuthenticatedEntity authenticatedEntity = merkleTree.put(entity);
    MembershipProof proof = authenticatedEntity.getMembershipProof();

    // creates a tampered proof with random root.
    Proof tamperedProof = new Proof(proof.getPath(), new Sha3256Hash(new byte[32]), proof.getIsLeftNode());
    AuthenticatedEntity tamperedAuthenticatedEntity = new MerkleTreeAuthenticatedEntity(
        tamperedProof,
        authenticatedEntity.type(),
        authenticatedEntity.getEntity());


    AuthenticatedEntityVerifier verifier = new AuthenticatedEntityVerifier();
    // authenticated entity must be verified.
    Assertions.assertTrue(verifier.verify(authenticatedEntity));
    // tampered authenticated entity must be failed.
    Assertions.assertFalse(verifier.verify(tamperedAuthenticatedEntity));
  }

  /**
   * Tests the proof verification fails when entity is changed.
   */
  @Test
  public void testManipulatedEntity() {
    MerkleTree merkleTree = MerkleTreeFixture.createMerkleTree(5);
    Assertions.assertEquals(merkleTree.size(), 5); // fixture sanity check.
    Entity entity = new EntityFixture();
    AuthenticatedEntity authenticatedEntity = merkleTree.put(entity);

    AuthenticatedEntity tamperedEntity = new MerkleTreeAuthenticatedEntity(
        (Proof) authenticatedEntity.getMembershipProof(),
        authenticatedEntity.type(),
        new EntityFixture());

    AuthenticatedEntityVerifier verifier = new AuthenticatedEntityVerifier();
    Assertions.assertTrue(verifier.verify(authenticatedEntity)); // original authenticated entity passes verification.
    Assertions.assertFalse(verifier.verify(tamperedEntity)); // tampered entity fails verification.
  }

  /**
   * Tests the proof fails verification when proof part of authenticated entity is changed.
   */
  @Test
  public void testManipulatedProof() {
    MerkleTree merkleTree = MerkleTreeFixture.createMerkleTree(5);
    Assertions.assertEquals(merkleTree.size(), 5); // fixture sanity check.

    Entity entity = new EntityFixture();
    AuthenticatedEntity authenticatedEntity = merkleTree.put(entity);
    MembershipProof proof = authenticatedEntity.getMembershipProof();

    AuthenticatedEntity tamperedEntity = new MerkleTreeAuthenticatedEntity(
        new Proof(Sha3256HashFixture.newSha3256HashArrayList(
            proof.getPath().size()),
            proof.getRoot(),
            proof.getIsLeftNode()),
        authenticatedEntity.type(),
        authenticatedEntity.getEntity());


    AuthenticatedEntityVerifier verifier = new AuthenticatedEntityVerifier();
    Assertions.assertTrue(verifier.verify(authenticatedEntity)); // original authenticated entity passes verification.
    Assertions.assertFalse(verifier.verify(tamperedEntity)); // tampered entity fails verification.
  }
}
