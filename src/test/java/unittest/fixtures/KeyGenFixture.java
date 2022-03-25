package unittest.fixtures;

/**
 * Encapsulates test utilities for ECDSA key generation.
 */
public class KeyGenFixture {
  public static model.crypto.ecdsa.EcdsaKeyGen newKeyGen() {
    return new model.crypto.ecdsa.EcdsaKeyGen();
  }
}
