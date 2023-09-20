package model.crypto;

import java.io.Serializable;

import model.lightchain.Identifier;

/**
 * Represents abstract data type for the cryptographic hash function used in LightChain.
 */
public abstract class Hash implements Serializable {
  public static final int EQUAL = 0;
  public static final int LESS = -1;
  public static final int GREATER = 1;
  /**
   * Actual value of hash in bytes.
   */
  private final byte[] bytes;

  public Hash(byte[] hashValue) {
    this.bytes = hashValue.clone();
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
  public abstract int compare(Hash other);

  /**
   * Converts a hash value to its corresponding identifier type.
   *
   * @return Identifier representation of the hash value.
   */
  public abstract Identifier toIdentifier();
}



