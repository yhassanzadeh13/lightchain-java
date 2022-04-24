package modules.ads;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import model.Entity;
import model.codec.EntityType;
import model.crypto.Sha3256Hash;
import model.lightchain.Identifier;
import modules.ads.merkletree.MerkleProof;
import modules.ads.merkletree.MerkleTree;
import modules.ads.merkletree.MerkleTreeAuthenticatedEntity;
import modules.ads.merkletree.MerkleTreeAuthenticatedEntityVerifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unittest.fixtures.*;

/**
 * Encapsulates tests for an authenticated and concurrent implementation of MerkleTree ADS.
 */
public class MerkleTreeTest {

  /**
   * Test putting and verifying a random entity in a random merkle tree.
   */
  @Test
  public void testVerificationNoArg() {
    testVerification(null, null);
  }

  /**
   * Tests both putting and getting the same random entity gives same proof and putting
   * another entity gives different proofs.
   */
  @Test
  public void testPutGetSameProofNoArg() {
    testPutGetSameProof(null, null);
  }

  /**
   * Tests putting an existing entity does not change the proof with a random entity and merkle tree.
   */
  @Test
  public void testPutExistingEntityNoArg() {
    testPutExistingEntity(null, null);
  }

  /**
   * Concurrently puts and gets entities and checks their proofs are correct (thread safety check).
   */
  @Test
  public void testConcurrentPutGetNoArg() {
    testConcurrentPutGet(null, null);
  }

  /**
   * Tests getting an entity that does not exist in the merkle tree throws IllegalArgumentException
   * with a random entity and merkle tree.
   */
  @Test
  public void testGetNonExistingEntityNoArg() {
    testGetNonExistingEntity(null, null);
  }

  /**
   * Tests inserting null throws IllegalArgumentException with a random merkle tree.
   */
  @Test
  public void testNullInsertionNoArg() {
    testNullInsertion(null);
  }

  /**
   * Tests the proof verification fails when root is changed with random entities and merkle tree.
   */
  @Test
  public void testManipulatedRootNoArg() {
    testManipulatedRoot(null, null);
  }

  /**
   * Tests the proof verification fails when entity is changed with random entity and merkle tree.
   */
  @Test
  public void testManipulatedEntityNoArg() {
    testManipulatedEntity(null, null);
  }

  /**
   * Tests the proof verification fails when entity is changed with random entity and merkle tree.
   */
  @Test
  public void testManipulatedProofNoArg() {
    testManipulatedProof(null, null);
  }

  /**
   * Generic function to test putting and verifying an entity in a merkle tree.
   *
   * @param entity the entity to put in the merkle tree
   * @param merkleTree the merkle tree to put the entity in
   */
  public static void testVerification(Entity entity, MerkleTree merkleTree) {
    entity = entity != null ? entity : new EntityFixture();
    merkleTree = merkleTree != null ? merkleTree : MerkleTreeFixture.createMerkleTree(5);
    Assertions.assertEquals(merkleTree.size(), 5); // fixture sanity check.
    merkleTree.put(entity);
    Assertions.assertEquals(merkleTree.size(), 6);

    AuthenticatedEntity authenticatedEntity = merkleTree.get(entity.id());
    MerkleTreeAuthenticatedEntityVerifier verifier = new MerkleTreeAuthenticatedEntityVerifier();
    Assertions.assertTrue(verifier.verify(authenticatedEntity));
  }

  /**
   * Generic function to test both putting and getting the same entity gives same proof
   * and putting another entity gives different proofs.
   *
   * @param e1 the entity to put in the merkle tree
   * @param merkleTree the merkle tree to put the entity in
   */
  public static void testPutGetSameProof(Entity e1, MerkleTree merkleTree) {
    merkleTree = merkleTree != null ? merkleTree : MerkleTreeFixture.createMerkleTree(5);
    Assertions.assertEquals(merkleTree.size(), 5); // fixture sanity check.
    e1 = e1 != null ? e1 : new EntityFixture();

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
   * Generic function which tests putting an existing entity does not change the proof.
   *
   * @param entity the entity to put in the merkle tree
   * @param merkleTree the merkle tree to put the entity in
   */
  public static void testPutExistingEntity(Entity entity, MerkleTree merkleTree) {
    entity = entity != null ? entity : new EntityFixture();
    merkleTree = merkleTree != null ? merkleTree : MerkleTreeFixture.createMerkleTree(5);
    Assertions.assertEquals(merkleTree.size(), 5); // fixture sanity check.

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
   * Generic function which concurrently puts and gets entities and checks their proofs are correct
   * (thread safety check).
   *
   * @param type the type of entity which is put in the merkle tree
   * @param merkleTree the merkle tree to put the entity in
   */
  public static void testConcurrentPutGet(String type, MerkleTree merkleTree) {
    int concurrencyDegree = 100;
    ArrayList<Entity> entities = new ArrayList<>();
    ArrayList<Identifier> ids = new ArrayList<>();

    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch putDone = new CountDownLatch(concurrencyDegree);
    CountDownLatch getDone = new CountDownLatch(concurrencyDegree);

    Thread[] putThreads = new Thread[concurrencyDegree];
    Thread[] getThreads = new Thread[concurrencyDegree];

    merkleTree = merkleTree != null ? merkleTree : MerkleTreeFixture.createMerkleTree(0);
    MerkleTree finalMerkleTree = merkleTree;
    Assertions.assertEquals(merkleTree.size(), 0); // fixture sanity check.

    for (int i = 0; i < concurrencyDegree; i++) {
      Entity entity;
      if (type == EntityType.TYPE_ACCOUNT) {
        entity = AccountFixture.newAccount(IdentifierFixture.newIdentifier());
      } else {
        entity = new EntityFixture();
      }
      entities.add(entity);
      ids.add(entity.id());
    }

    // put
    for (int i = 0; i < concurrencyDegree; i++) {
      Entity entity = entities.get(i);
      putThreads[i] = new Thread(() -> {
        try {
          finalMerkleTree.put(entity);
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
          AuthenticatedEntity authenticatedEntity = finalMerkleTree.get(id);
          MerkleTreeAuthenticatedEntityVerifier verifier = new MerkleTreeAuthenticatedEntityVerifier();
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
   * Generic function which tests getting an entity that does not exist in the merkle tree
   * throws IllegalArgumentException.
   *
   * @param entity the entity to put in the merkle tree
   * @param merkleTree the merkle tree to put the entity in
   */
  public static void testGetNonExistingEntity(Entity entity, MerkleTree merkleTree) {
    merkleTree = merkleTree != null ? merkleTree : MerkleTreeFixture.createMerkleTree(5);
    Assertions.assertEquals(merkleTree.size(), 5); // fixture sanity check.
    entity = entity != null ? entity : new EntityFixture();
    Entity finalEntity = entity;
    MerkleTree finalMerkleTree = merkleTree;
    Assertions.assertThrows(IllegalArgumentException.class, () -> finalMerkleTree.get(finalEntity.id()));
  }

  /**
   * Generic function which tests inserting null throws IllegalArgumentException.
   *
   * @param merkleTree the merkle tree to put null
   */
  public static void testNullInsertion(MerkleTree merkleTree) {
    merkleTree = merkleTree != null ? merkleTree : MerkleTreeFixture.createMerkleTree(5);
    Assertions.assertEquals(merkleTree.size(), 5); // fixture sanity check.
    MerkleTree finalMerkleTree = merkleTree;
    Assertions.assertThrows(IllegalArgumentException.class, () -> finalMerkleTree.put(null));
  }

  /**
   * Generic function which tests the proof verification fails when root is changed.
   *
   * @param entity the entity to put in the merkle tree
   * @param merkleTree the merkle tree to put the entity in
   */
  public static void testManipulatedRoot(Entity entity, MerkleTree merkleTree) {
    entity = entity != null ? entity : new EntityFixture();
    merkleTree = merkleTree != null ? merkleTree : MerkleTreeFixture.createMerkleTree(5);
    Assertions.assertEquals(merkleTree.size(), 5); // fixture sanity check.
    AuthenticatedEntity authenticatedEntity = merkleTree.put(entity);
    MembershipProof proof = authenticatedEntity.getMembershipProof();

    // creates a tampered proof with random root.
    MerkleProof tamperedProof = new MerkleProof(proof.getPath(), new Sha3256Hash(new byte[32]), proof.getIsLeftNode());
    AuthenticatedEntity tamperedAuthenticatedEntity = new MerkleTreeAuthenticatedEntity(
            tamperedProof,
            authenticatedEntity.type(),
            authenticatedEntity.getEntity());

    MerkleTreeAuthenticatedEntityVerifier verifier = new MerkleTreeAuthenticatedEntityVerifier();
    // authenticated entity must be verified.
    Assertions.assertTrue(verifier.verify(authenticatedEntity));
    // tampered authenticated entity must be failed.
    Assertions.assertFalse(verifier.verify(tamperedAuthenticatedEntity));
  }

  /**
   * Generic function which tests the proof verification fails when entity is changed.
   *
   * @param entity the entity to put in the merkle tree
   * @param merkleTree the merkle tree to put the entity in
   */
  public static void testManipulatedEntity(Entity entity, MerkleTree merkleTree) {
    entity = entity != null ? entity : new EntityFixture();
    merkleTree = merkleTree != null ? merkleTree : MerkleTreeFixture.createMerkleTree(5);
    Assertions.assertEquals(merkleTree.size(), 5); // fixture sanity check.
    AuthenticatedEntity authenticatedEntity = merkleTree.put(entity);

    AuthenticatedEntity tamperedEntity = new MerkleTreeAuthenticatedEntity(
            (MerkleProof) authenticatedEntity.getMembershipProof(),
            authenticatedEntity.type(),
            new EntityFixture());

    MerkleTreeAuthenticatedEntityVerifier verifier = new MerkleTreeAuthenticatedEntityVerifier();
    Assertions.assertTrue(verifier.verify(authenticatedEntity)); // original authenticated entity passes verification.
    Assertions.assertFalse(verifier.verify(tamperedEntity)); // tampered entity fails verification.
  }

  /**
   * Generic function which tests the proof fails verification when proof part of authenticated entity is changed.
   *
   * @param entity the entity to put in the merkle tree
   * @param merkleTree the merkle tree to put the entity in
   */
  public static void testManipulatedProof(Entity entity, MerkleTree merkleTree) {
    entity = entity != null ? entity : new EntityFixture();
    merkleTree = merkleTree != null ? merkleTree : MerkleTreeFixture.createMerkleTree(5);
    Assertions.assertEquals(merkleTree.size(), 5); // fixture sanity check.
    AuthenticatedEntity authenticatedEntity = merkleTree.put(entity);
    MembershipProof proof = authenticatedEntity.getMembershipProof();

    AuthenticatedEntity tamperedEntity = new MerkleTreeAuthenticatedEntity(
            new MerkleProof(Sha3256HashFixture.newSha3256HashArrayList(
                    proof.getPath().size()),
                    proof.getRoot(),
                    proof.getIsLeftNode()),
            authenticatedEntity.type(),
            authenticatedEntity.getEntity());

    MerkleTreeAuthenticatedEntityVerifier verifier = new MerkleTreeAuthenticatedEntityVerifier();
    Assertions.assertTrue(verifier.verify(authenticatedEntity)); // original authenticated entity passes verification.
    Assertions.assertFalse(verifier.verify(tamperedEntity)); // tampered entity fails verification.
  }
}
