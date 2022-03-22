package crypto;

import model.crypto.ecdsa.EcdsaKeyGen;
import model.crypto.ecdsa.EcdsaPrivateKey;
import model.crypto.ecdsa.EcdsaPublicKey;
import model.crypto.ecdsa.EcdsaSignature;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unittest.fixtures.*;

public class EcdsaTest {

  /**
   * Round trip test of ECDSA signing and verification.
   */
  @Test
  public void TestVerificationRoundTrip() {
    EntityFixture e = new EntityFixture();
    EcdsaKeyGen keyGen = KeyGenFixture.newKeyGen();
    EcdsaPrivateKey ecdsaPrivateKey = new EcdsaPrivateKey(keyGen.getPrivateKey().getPrivateKeyBytes());
    EcdsaSignature signature = new EcdsaSignature(ecdsaPrivateKey.signEntity(e).getBytes(), e.id());
    EcdsaPublicKey publicKey = new EcdsaPublicKey(keyGen.getPublicKey().getPublicKeyBytes());
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
    EcdsaPrivateKey ecdsaPrivateKey = new EcdsaPrivateKey(keyGen.getPrivateKey().getPrivateKeyBytes());
    EcdsaSignature signature = new EcdsaSignature(ecdsaPrivateKey.signEntity(e).getBytes(), e.id());
    EcdsaPublicKey publicKey = new EcdsaPublicKey(keyGen.getPublicKey().getPublicKeyBytes());
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
    EcdsaPrivateKey ecdsaPrivateKey = new EcdsaPrivateKey(keyGen.getPrivateKey().getPrivateKeyBytes());
    EcdsaSignature signature = new EcdsaSignature(ecdsaPrivateKey.signEntity(e).getBytes(), e.id());
    byte[] publicKeyManipulatedBytes = keyGenManipulated.getPublicKey().getPublicKeyBytes();
    EcdsaPublicKey publicKeyManipulated = new EcdsaPublicKey(publicKeyManipulatedBytes);
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
    byte[] privateKeyManipulatedBytes = keyGenManipulated.getPrivateKey().getPrivateKeyBytes();
    EcdsaPrivateKey ecdsaPrivateKey = new EcdsaPrivateKey(privateKeyManipulatedBytes);
    EcdsaSignature signature = new EcdsaSignature(ecdsaPrivateKey.signEntity(e).getBytes(), e.id());
    EcdsaPublicKey publicKey = new EcdsaPublicKey(keyGen.getPublicKey().getPublicKeyBytes());
    Assertions.assertFalse(publicKey.verifySignature(e, signature));
  }

}
