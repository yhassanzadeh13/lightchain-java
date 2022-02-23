package model.crypto;

/**
 * Represents abstract data type for the cryptographic digital signature used in LightChain.
 */
public abstract class Signature {
  /**
   * The signature value in bytes.
   */
  private final byte[] bytes;

  public Signature(byte[] bytes) {
    this.bytes = bytes;
  }
}
