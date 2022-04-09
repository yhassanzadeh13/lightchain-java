package network.p2p.Fixtures;

import model.crypto.Sha3256Hash;

/**
 * Encapsulates test utilities for SHA3-256 hashing.
 */
public class Sha3256HashFixture {
  /**
   * Generates random looking SHA3-256 hash value.
   *
   * @return a randomly looking SHA3-256 hash value.
   */
  public static Sha3256Hash newSha3256Hash() {
    byte[] bytes = Bytes.byteArrayFixture(Sha3256Hash.Size);
    return new Sha3256Hash(bytes);
  }

  /**
   * Generates an array of random looking SHA3-256 hash values.
   *
   * @return an array filled with randomly generated SHA3-256 hash values.
   */
  public static Sha3256Hash[] newSha3256HashArray() {
    Sha3256Hash[] hashArray = new Sha3256Hash[32];
    for (int i = 0; i < 32; i++) {
      hashArray[i] = newSha3256Hash();
    }
    return hashArray;
  }
}
