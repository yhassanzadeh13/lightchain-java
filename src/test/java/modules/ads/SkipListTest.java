package modules.ads;


import modules.ads.skiplist.SkipList;
import modules.ads.skiplist.Verifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unittest.fixtures.EntityFixture;
import unittest.fixtures.SkipListFixture;

public class SkipListTest {

  // TODO: writing tests to cover
  // 1. When putting a unique entity into skip list, we can recover it.
  // 2. Proof of membership for putting and getting an entity is the same. +
  // 3. Putting an already existing entity does not change its membership proof. +
  // 4. Putting 100 distinct entities concurrently inserts all of them into skip list with correct membership proofs, and
  //    also, makes them all retrievable with correct membership proofs.
  // 5. Getting non-existing identifiers returns null. +
  // 7. Putting null returns null. +
  // 8. Tampering with root identifier of an authenticated entity fails its verification.
  // 9. Tampering with entity of an authenticated entity fails its verification.
  // 10. Tampering with proof of an authenticated entity fails its verification.

  @Test
  public void TestVerification() { // Do not work always
    SkipList skipList = SkipListFixture.createSkipList(0);
    EntityFixture entityFixture = new EntityFixture();
    skipList.put(entityFixture);
    AuthenticatedEntity authenticatedEntity = skipList.get(entityFixture);
    Verifier verifier = new Verifier();
    Assertions.assertTrue(verifier.verify(authenticatedEntity));
    System.out.println(authenticatedEntity.getMembershipProof());
  }

  @Test
  public void TestRecoverEntity() { // Test 1
    SkipList skipList = SkipListFixture.createSkipList(5);
    EntityFixture entityFixture = new EntityFixture();
    skipList.put(entityFixture);
    AuthenticatedEntity authenticatedEntity = skipList.get(entityFixture);
    Assertions.assertEquals(entityFixture, authenticatedEntity.getEntity());
  }

  @Test
  public void TestInsertGetSameProof() { // Test 2
    SkipList skipList = SkipListFixture.createSkipList(5);
    EntityFixture entityFixture = new EntityFixture();
    AuthenticatedEntity authenticatedEntityPut = skipList.put(entityFixture);
    MembershipProof proofPut = authenticatedEntityPut.getMembershipProof();
    AuthenticatedEntity authenticatedEntityGet = skipList.get(entityFixture);
    MembershipProof proofGet = authenticatedEntityGet.getMembershipProof();
    Assertions.assertEquals(proofPut, proofGet);
  }

  @Test
  public void TestPutExistingEntity() { // Test 3
    SkipList skipList = SkipListFixture.createSkipList(5);
    EntityFixture entityFixture = new EntityFixture();
    AuthenticatedEntity authenticatedEntityPut = skipList.put(entityFixture);
    MembershipProof proofPut = authenticatedEntityPut.getMembershipProof();
    AuthenticatedEntity authenticatedEntityPutAgain = skipList.put(entityFixture);
    MembershipProof proofPutAgain = authenticatedEntityPutAgain.getMembershipProof();
    Assertions.assertEquals(proofPut, proofPutAgain);
  }

  @Test
  public void TestPutNonExistingEntity() { // Test 5
    SkipList skipList = SkipListFixture.createSkipList(5);
    EntityFixture entityFixture = new EntityFixture();
    AuthenticatedEntity authenticatedEntity = skipList.get(entityFixture);
    Assertions.assertNull(authenticatedEntity);
  }

  @Test
  public void TestNullInsertion() { // Test 7
    SkipList skipList = SkipListFixture.createSkipList(5);
    AuthenticatedEntity authenticatedEntity = skipList.put(null);
    Assertions.assertNull(authenticatedEntity);
  }
}
