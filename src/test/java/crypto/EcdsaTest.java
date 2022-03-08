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
    PublicKeyFixture publicKey = new PublicKeyFixture(keyGen.getPublicKey().getPublicKeyBytes());
    SignatureFixture signature = new SignatureFixture(privateKey.signEntity(e).getBytes(), e.id());
    boolean test = publicKey.verifySignature(e, signature);
    Assertions.assertTrue(test);
  }
}
