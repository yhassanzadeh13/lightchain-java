package crypto;

import model.crypto.ecdsa.EcdsaKeyGen;
import model.crypto.ecdsa.EcdsaPrivateKey;
import model.crypto.ecdsa.EcdsaPublicKey;
import model.crypto.ecdsa.EcdsaSignature;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unittest.fixtures.EntityFixture;
import unittest.fixtures.KeyGenFixture;

public class EcdsaTest {

  /**
   * Round trip test of ECDSA signing and verification.
   */
  @Test
  public void TestVerificationRoundTrip() {
    EntityFixture e = new EntityFixture();
    EcdsaKeyGen keyGen = KeyGenFixture.newKeyGen();
    EcdsaPrivateKey ecdsaPrivateKey = keyGen.getPrivateKey();
    EcdsaSignature signature = new EcdsaSignature(ecdsaPrivateKey.signEntity(e).getBytes(), e.id());
    EcdsaPublicKey publicKey = keyGen.getPublicKey();
    Assertions.assertTrue(publicKey.verifySignature(e, signature));
  }

  /**
   * Test of ECDSA signing and verification with manipulated Entity.
   */
  @Test
  public void TestEntityChange() {
    EntityFixture e = new EntityFixture();
    EntityFixture entityManipulated = new EntityFixture();
    EcdsaKeyGen keyGen = KeyGenFixture.newKeyGen();
    EcdsaPrivateKey ecdsaPrivateKey = keyGen.getPrivateKey();
    EcdsaSignature signature = new EcdsaSignature(ecdsaPrivateKey.signEntity(e).getBytes(), e.id());
    EcdsaPublicKey publicKey = keyGen.getPublicKey();
    Assertions.assertFalse(publicKey.verifySignature(entityManipulated, signature));
  }

  /**
   * Test of ECDSA signing and verification with manipulated PublicKey.
   */
  @Test
  public void TestPublicKeyChange() {
    EntityFixture e = new EntityFixture();
    EcdsaKeyGen keyGen = KeyGenFixture.newKeyGen();
    EcdsaKeyGen keyGenManipulated = KeyGenFixture.newKeyGen();
    EcdsaPrivateKey ecdsaPrivateKey = keyGen.getPrivateKey();
    EcdsaSignature signature = new EcdsaSignature(ecdsaPrivateKey.signEntity(e).getBytes(), e.id());
    EcdsaPublicKey publicKeyManipulated = keyGenManipulated.getPublicKey();
    Assertions.assertFalse(publicKeyManipulated.verifySignature(e, signature));
  }

  /**
   * Test of ECDSA signing and verification with manipulated Signature.
   */
  @Test
  public void TestSignatureChange() {
    EntityFixture e = new EntityFixture();
    EcdsaKeyGen keyGen = KeyGenFixture.newKeyGen();
    EcdsaKeyGen keyGenManipulated = KeyGenFixture.newKeyGen();
    EcdsaPrivateKey ecdsaPrivateKey = keyGenManipulated.getPrivateKey();
    EcdsaSignature signature = new EcdsaSignature(ecdsaPrivateKey.signEntity(e).getBytes(), e.id());
    EcdsaPublicKey publicKey = keyGen.getPublicKey();
    Assertions.assertFalse(publicKey.verifySignature(e, signature));
  }

}
