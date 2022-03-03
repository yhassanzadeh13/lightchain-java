package model.crypto;

import model.lightchain.Identifier;

/**
 * Represents SHA3-256 data type which extends abstract Hash data type for
 * the cryptographic hash function used in LightChain.
 */
public class Sha3256Hash extends Hash {
  private final byte[] hashBytes;

  /**
   * Constructs a SHA3-256 hash object from a byte array.
   *
   * @param hashValue the byte array to construct the hash from
   */
  public Sha3256Hash(byte[] hashValue) {
    super(hashValue);
    if (hashValue.length != 32) {
      throw new IllegalArgumentException("Hash value must be 32 bytes long.");
    }
    this.hashBytes = hashValue.clone();
  }

  /**
   * Constructs a SHA3-256 hash object from an identifier.
   *
   * @param identifier the identifier to construct the hash from
   */
  public Sha3256Hash(Identifier identifier) {
    super(identifier);
    if (identifier.getBytes().length != 32) {
      throw new IllegalArgumentException("Identifier must be 32 bytes long.");
    }
    this.hashBytes = identifier.getBytes();
  }

  @Override
  public int compare(Hash other) {
    return this.toIdentifier().comparedTo(other.toIdentifier());
  }

  @Override
  public Identifier toIdentifier() {
    return new Identifier(this.hashBytes);
  }
}
