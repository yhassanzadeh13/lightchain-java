package model.crypto;

import model.Entity;

/**
 * Represents abstract data type for the cryptographic public key used in LightChain.
 */
public abstract class PublicKey {
  protected final byte[] bytes;

  public PublicKey(byte[] bytes) {
    this.bytes = bytes.clone();
  }

  /**
   * Implements signature verification.
   *
   * @param e entity that carries a signature on.
   * @param s digital signature over the entity.
   * @return true if s carries a valid signature over e against this public key, false otherwise.
   */
  public abstract boolean verifySignature(Entity e, Signature s) throws IllegalStateException;
}
