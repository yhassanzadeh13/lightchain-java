        EncodedEntity encodedEntity1 = encoder.encode(testEntity1);
package crypto;

import model.codec.EncodedEntity;
import model.crypto.Hash;
import model.lightchain.Identifier;
import modules.codec.JsonEncoder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unittest.fixtures.EntityFixture;

public class Sha3256HasherTest {

  /**
   * Test if the hash is 32 bytes long
   */
  @Test
  public void TestHashLength() {
    EntityFixture testEntity = new EntityFixture();
    JsonEncoder encoder = new JsonEncoder();
    EncodedEntity encodedEntity = encoder.encode(testEntity);
    Sha3256Hasher hasher = new Sha3256Hasher();
    Hash hash = hasher.computeHash(encodedEntity);
    Identifier identifier = hash.toIdentifier();
    byte[] bytes = identifier.getBytes();
    Assertions.assertEquals(32, bytes.length);
  }

  /**
   * Test if the hash is the same for the same entity
   */
  @Test
  public void TestHashingSameEntity() {
    EntityFixture testEntity = new EntityFixture();
    JsonEncoder encoder = new JsonEncoder();
    EncodedEntity encodedEntity = encoder.encode(testEntity);
    Sha3256Hasher hasher = new Sha3256Hasher();
    Hash hash1 = hasher.computeHash(encodedEntity);
    for (int i = 0; i < 100; i++) {
      Hash hash2 = hasher.computeHash(encodedEntity);
      Assertions.assertEquals(hash1.compare(hash2), Hash.EQUAL);
    }
  }

  /**
   * Test if the hash is different for different entities
   */
  @Test
  public void TestHashingDifferentEntities() {
    JsonEncoder encoder = new JsonEncoder();
    Sha3256Hasher hasher = new Sha3256Hasher();
    EntityFixture testEntity1 = new EntityFixture();
    EntityFixture testEntity2 = new EntityFixture();
    EncodedEntity encodedEntity1 = encoder.encode(testEntity1);
    EncodedEntity encodedEntity2 = encoder.encode(testEntity2);
    Hash hash1 = hasher.computeHash(encodedEntity1);
    Hash hash2 = hasher.computeHash(encodedEntity2);
    for (int i = 0; i < 100; i++) {
      Assertions.assertNotEquals(hash1.compare(hash2), Hash.EQUAL);
      hash1 = hash2;
      testEntity2 = new EntityFixture();
      encodedEntity2 = encoder.encode(testEntity2);
      hash2 = hasher.computeHash(encodedEntity2);
    }
  }
}
