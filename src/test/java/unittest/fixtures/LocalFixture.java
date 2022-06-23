package unittest.fixtures;

import model.crypto.PrivateKey;
import model.lightchain.Identifier;
import model.local.Local;

/**
 * Encapsulates creating a local for testing.
 */
public class LocalFixture {
  /**
   * Creates and returns a new local object with random identifier and private key.
   *
   * @return new local object with random identifier and private key.
   */
  public static Local newLocal() {
    Identifier localId = IdentifierFixture.newIdentifier();
    PrivateKey localPrivateKey = KeyGenFixture.newKeyGen().getPrivateKey();
    return new Local(localId, localPrivateKey);
  }
}
