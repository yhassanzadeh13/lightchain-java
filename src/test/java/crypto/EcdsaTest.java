package crypto;

import model.crypto.ecdsa.EcdsaKeyGen;
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
    EcdsaKeyGen keyGen = new KeyGenFixture();
    PrivateKeyFixture privateKey = new PrivateKeyFixture(keyGen.getPrivateKey().getPrivateKeyBytes());
    SignatureFixture signature = new SignatureFixture(privateKey.signEntity(e).getBytes(), e.id());
    PublicKeyFixture publicKey = new PublicKeyFixture(keyGen.getPublicKey().getPublicKeyBytes());
    Assertions.assertTrue(publicKey.verifySignature(e, signature));
  }

  /**
   * Test of ECDSA signing and verification with manipulated Entity.
   */
  @Test
  public void TestEntityChange() {
    EntityFixture e = new EntityFixture();
    EntityFixture entityManipulated = new EntityFixture();
    EcdsaKeyGen keyGen = new KeyGenFixture();
    PrivateKeyFixture privateKey = new PrivateKeyFixture(keyGen.getPrivateKey().getPrivateKeyBytes());
    SignatureFixture signature = new SignatureFixture(privateKey.signEntity(e).getBytes(), e.id());
    PublicKeyFixture publicKey = new PublicKeyFixture(keyGen.getPublicKey().getPublicKeyBytes());
    boolean test = publicKey.verifySignature(entityManipulated, signature);
    Assertions.assertFalse(test);
  }

  /**
   * Test of ECDSA signing and verification with manipulated PublicKey.
   */
  @Test
  public void TestPublicKeyChange() {
    EntityFixture e = new EntityFixture();
    EcdsaKeyGen keyGen = new KeyGenFixture();
    EcdsaKeyGen keyGenManipulated = new KeyGenFixture();
    PrivateKeyFixture privateKey = new PrivateKeyFixture(keyGen.getPrivateKey().getPrivateKeyBytes());
    SignatureFixture signature = new SignatureFixture(privateKey.signEntity(e).getBytes(), e.id());
    byte[] publicKeyManipulatedBytes = keyGenManipulated.getPublicKey().getPublicKeyBytes();
    PublicKeyFixture publicKeyManipulated = new PublicKeyFixture(publicKeyManipulatedBytes);
    boolean test = publicKeyManipulated.verifySignature(e, signature);
    Assertions.assertFalse(test);
  }

  /**
   * Test of ECDSA signing and verification with manipulated Signature.
   */
  @Test
  public void TestSignatureChange() {
    EntityFixture e = new EntityFixture();
    EcdsaKeyGen keyGen = new KeyGenFixture();
    EcdsaKeyGen keyGenManipulated = new KeyGenFixture();
    byte[] privateKeyManipulatedBytes = keyGenManipulated.getPrivateKey().getPrivateKeyBytes();
    PrivateKeyFixture privateKeyManipulated = new PrivateKeyFixture(privateKeyManipulatedBytes);
    SignatureFixture signature = new SignatureFixture(privateKeyManipulated.signEntity(e).getBytes(), e.id());
    PublicKeyFixture publicKey = new PublicKeyFixture(keyGen.getPublicKey().getPublicKeyBytes());
    boolean test = publicKey.verifySignature(e, signature);
    Assertions.assertFalse(test);
  }

}
