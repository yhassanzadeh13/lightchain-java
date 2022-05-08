package modules.ads;

import model.Entity;
import modules.ads.merkletree.MerkleTree;
import modules.ads.merkletree.MerkleTreeAuthenticatedEntityVerifier;
import modules.ads.mtrie.MerklePatriciaTrie;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unittest.fixtures.EntityFixture;
import unittest.fixtures.MerklePatriciaTrieFixture;
import unittest.fixtures.MerkleTreeFixture;

public class MerklePatriciaTrieTest {
  /**
   * A basic test for one sequential put and get operations.
   */
  @Test
  public void testVerification() {
    MerklePatriciaTrie merklePatriciaTrie = MerklePatriciaTrieFixture.createMerklePatriciaTree(0);
    Assertions.assertEquals(merklePatriciaTrie.size(), 0); // fixture sanity check.

    Entity entity = new EntityFixture();
    merklePatriciaTrie.put(entity);
    Assertions.assertEquals(merklePatriciaTrie.size(), 1);

    AuthenticatedEntity authenticatedEntity = merklePatriciaTrie.get(entity.id());
    MerkleTreeAuthenticatedEntityVerifier verifier = new MerkleTreeAuthenticatedEntityVerifier();
    Assertions.assertTrue(verifier.verify(authenticatedEntity));
  }
}
