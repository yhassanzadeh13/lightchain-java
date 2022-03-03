package model.crypto;

import model.lightchain.Identifier;

/**
 * Represents SHA3-256 data type which extends abstract Hash data type for
 * the cryptographic hash function used in LightChain.
 */
public class Sha3256Hash extends Hash {
  private final byte[] bytes;

  public Sha3256Hash(byte[] hashValue) {
    super(hashValue);
    if (hashValue.length != 32) {
      throw new IllegalArgumentException("Hash value must be 32 bytes long.");
    }
    this.bytes = hashValue.clone();
  }

  public Sha3256Hash(Identifier identifier) {
    super(identifier);
    if (identifier.getBytes().length != 32) {
      throw new IllegalArgumentException("Identifier must be 32 bytes long.");
    }
    this.bytes = identifier.getBytes();
  }

  @Override
  public int compare(Hash other) {
    return this.toIdentifier().comparedTo(other.toIdentifier());
  }

  @Override
  public Identifier toIdentifier() {
    return new Identifier(this.bytes);
  }
}
