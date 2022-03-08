package crypto;

import model.codec.EncodedEntity;
import model.crypto.Sha3256Hash;
import model.crypto.ecdsa.EcdsaKeyGen;
import modules.codec.JsonEncoder;
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
   * Test of ECDSA signing and verification with manipulated PublicKey.
   */
  @Test
  public void TestPublicKeyChange() {
    EntityFixture e = new EntityFixture();
    EcdsaKeyGen keyGen = new KeyGenFixture();
    PrivateKeyFixture privateKey = new PrivateKeyFixture(keyGen.getPrivateKey().getPrivateKeyBytes());
    SignatureFixture signature = new SignatureFixture(privateKey.signEntity(e).getBytes(), e.id());
    byte[] publicKeyBytes = keyGen.getPublicKey().getPublicKeyBytes();
    publicKeyBytes[0] = (byte) (publicKeyBytes[0] + 1);
    PublicKeyFixture publicKey = new PublicKeyFixture(publicKeyBytes);
    boolean test = publicKey.verifySignature(e, signature);
    Assertions.assertFalse(test);
  }

  /**
   * Test of ECDSA signing and verification with manipulated Signature.
   */
  @Test
  public void TestSignatureChange() {
    EntityFixture e = new EntityFixture();
    EcdsaKeyGen keyGen = new KeyGenFixture();
    PrivateKeyFixture privateKey = new PrivateKeyFixture(keyGen.getPrivateKey().getPrivateKeyBytes());
    byte[] signatureBytes = privateKey.signEntity(e).getBytes();
    signatureBytes[0] = (byte) (signatureBytes[0] + 1);
    SignatureFixture signature = new SignatureFixture(signatureBytes, e.id());
    PublicKeyFixture publicKey = new PublicKeyFixture(keyGen.getPublicKey().getPublicKeyBytes());
    boolean test = publicKey.verifySignature(e, signature);
    Assertions.assertFalse(test);
  }

}
