    }
package model.crypto;

import java.util.Arrays;

import model.lightchain.Identifier;

/**
 * Represents SHA3-256 data type which extends abstract Hash data type for
 * the cryptographic hash function used in LightChain.
 */
public class Sha3256Hash extends Hash {
  public static final int Size = 32;
  private final byte[] hashBytes;

  /**
   * Constructs a SHA3-256 hash object from a byte array.
   *
   * @param hashValue the byte array to construct the hash from
   */
  public Sha3256Hash(byte[] hashValue) {
    super(hashValue);
    if (hashValue.length != Size) {
      throw new IllegalArgumentException("hash value must be 32 bytes long");
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
    if (identifier.getBytes().length != Size) {
      throw new IllegalArgumentException("identifier must be 32 bytes long");
    }
    this.hashBytes = identifier.getBytes();
  }

  public byte[] getHashBytes() {
    return hashBytes;
  }

  @Override
  public int compare(Hash other) {
    return this.toIdentifier().comparedTo(other.toIdentifier());
  }

  @Override
  public Identifier toIdentifier() {
    return new Identifier(this.hashBytes);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Sha3256Hash that = (Sha3256Hash) o;
    return Arrays.equals(hashBytes, that.hashBytes);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(hashBytes);
  }
}
