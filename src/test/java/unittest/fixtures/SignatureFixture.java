package unittest.fixtures;

import model.crypto.Signature;
import model.crypto.ecdsa.EcdsaSignature;
import model.lightchain.Identifier;

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

  /**
   * Generates a random signature for testing.
   *
   * @param signerId identifier of signer.
   * @return 32-bits randomly generated ECDSA signature.
   */
  public static Signature newSignatureFixture(Identifier signerId) {
    return new EcdsaSignature(Bytes.byteArrayFixture(32), signerId);
  }
}
