package model.crypto;

import model.lightchain.Identifier;

/**
 * Represents abstract data type for the cryptographic hash function used in LightChain.
 */
public abstract class Hash {
  /**
   * Actual value of hash in bytes.
   */
  private final byte[] bytes;

  public Hash(final byte[] hashValue) {
    this.bytes = hashValue;
  }

  public Hash(Identifier identifier) {
    this.bytes = identifier.getBytes();
  }

  /**
   * Compares this hash value with the other hash value.
   *
   * @param other the other hash object to compare.
   * @return +1 if this hash value is greater than other hash value, 0 if both hash values are equal and -1 otherwise.
   */
  abstract boolean compare(Hash other);

  /**
   * Converts a hash value to its corresponding identifier type.
   *
   * @return Identifier representation of the hash value.
   */
  abstract Identifier toIdentifier();
}



