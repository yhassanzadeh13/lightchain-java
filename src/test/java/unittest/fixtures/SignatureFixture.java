package unittest.fixtures;

import model.crypto.Signature;
import model.crypto.ecdsa.EcdsaSignature;

/**
 * Encapsulates randomly generated signature objects for testing.
 */
public class SignatureFixture {
  /**
   * Generates a random signature for testing.
   *
   * @return 32-bits randomly generated ECDSA signature.
   */
  public static Signature newSignatureFixture() {
    return new EcdsaSignature(Bytes.byteArrayFixture(32), IdentifierFixture.newIdentifier());
  }
}
