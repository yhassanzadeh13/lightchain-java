package modules.ads;

import model.crypto.Sha3256Hash;
import modules.ads.merkletree.MerkleTree;
import modules.ads.merkletree.Verifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unittest.fixtures.EntityFixture;
import unittest.fixtures.MerkleTreeFixture;

import java.util.ArrayList;

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
    EntityFixture entityFixture = new EntityFixture();
    merkleTree.put(entityFixture);
    AuthenticatedEntity authenticatedEntity = merkleTree.get(entityFixture);
    Verifier verifier = new Verifier();
    Assertions.assertTrue(verifier.verify(authenticatedEntity));
  }

  @Test
  public void TestPutGetSameProof() { // Test 2
    MerkleTree merkleTree = MerkleTreeFixture.createSkipList(5);
    EntityFixture entityFixture = new EntityFixture();
    AuthenticatedEntity authenticatedEntityPut = merkleTree.put(entityFixture);
    MembershipProof proofPut = authenticatedEntityPut.getMembershipProof();
    AuthenticatedEntity authenticatedEntityGet = merkleTree.get(entityFixture);
    MembershipProof proofGet = authenticatedEntityGet.getMembershipProof();
    EntityFixture entityFixture2 = new EntityFixture();
    AuthenticatedEntity authenticatedEntityPut2 = merkleTree.put(entityFixture2);
    MembershipProof proofPut2 = authenticatedEntityPut2.getMembershipProof();
    Assertions.assertEquals(proofPut, proofGet);
    Assertions.assertNotEquals(proofPut, proofPut2);
  }

  @Test
  public void TestPutExistingEntity() { // Test 3
    MerkleTree merkleTree = MerkleTreeFixture.createSkipList(5);
    EntityFixture entityFixture = new EntityFixture();
    AuthenticatedEntity authenticatedEntityPut = merkleTree.put(entityFixture);
    MembershipProof proofPut = authenticatedEntityPut.getMembershipProof();
    AuthenticatedEntity authenticatedEntityPutAgain = merkleTree.put(entityFixture);
    MembershipProof proofPutAgain = authenticatedEntityPutAgain.getMembershipProof();
    Assertions.assertEquals(proofPut, proofPutAgain);
  }

  @Test
  public void TestGetNonExistingEntity() { // Test 5
    MerkleTree merkleTree = MerkleTreeFixture.createSkipList(5);
    EntityFixture entityFixture = new EntityFixture();
    AuthenticatedEntity authenticatedEntity = merkleTree.get(entityFixture);
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
    EntityFixture entityFixture = new EntityFixture();
    AuthenticatedEntity authenticatedEntity = merkleTree.put(entityFixture);
    MembershipProof proof = authenticatedEntity.getMembershipProof();
    proof.setRoot(new Sha3256Hash(new byte[32]));
    authenticatedEntity.setMembershipProof(proof);
    Verifier verifier = new Verifier();
    Assertions.assertFalse(verifier.verify(authenticatedEntity));
  }

  @Test
  public void TestManipulatedEntity() { // Test 9
    MerkleTree merkleTree = MerkleTreeFixture.createSkipList(5);
    EntityFixture entityFixture = new EntityFixture();
    AuthenticatedEntity authenticatedEntity = merkleTree.put(entityFixture);
    authenticatedEntity.setEntity(new EntityFixture());
    Verifier verifier = new Verifier();
    Assertions.assertFalse(verifier.verify(authenticatedEntity));
  }

  @Test
  public void TestManipulatedProof() { // Test 10
    MerkleTree merkleTree = MerkleTreeFixture.createSkipList(5);
    EntityFixture entityFixture = new EntityFixture();
    AuthenticatedEntity authenticatedEntity = merkleTree.put(entityFixture);
    MembershipProof proof = authenticatedEntity.getMembershipProof();
    ArrayList<Sha3256Hash> proofPath = proof.getPath();
    proofPath.add(new Sha3256Hash(new byte[32]));
    proof.setPath(proofPath);
    authenticatedEntity.setMembershipProof(proof);
    Verifier verifier = new Verifier();
    Assertions.assertFalse(verifier.verify(authenticatedEntity));
  }
}
