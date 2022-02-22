package model.crypto;

public abstract class Hash {
  /**
   * Actual value of hash in bytes.
   */
  private final byte[] bytes;

  protected Hash(byte[] hashValue) {
    this.bytes = hashValue;
  }

  /**
   * Compares this hash value with the other hash value.
   * @param other the other hash object to compare.
   * @return +1 if this hash value is greater than other hash value, 0 if both hash values are equal and -1 otherwise.
   */
  abstract boolean Compare(Hash other);
}



