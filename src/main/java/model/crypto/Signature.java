package model.crypto;

import model.Entity;
import model.lightchain.Identifier;

/**
 * Represents abstract data type for the cryptographic digital signature used in LightChain.
 */
public abstract class Signature extends Entity {
  /**
   * The signature value in bytes.
   */
  private final byte[] bytes;

  /**
   * Identifier of node that signed transaction.
   */
  private final Identifier signerId;

  public Signature(byte[] bytes, Identifier signerId) {
    this.bytes = bytes.clone();
    this.signerId = signerId;
  }

  public Identifier getSignerId() {
    return signerId;
  }

  public byte[] getBytes() {
    return bytes.clone();
  }
}
