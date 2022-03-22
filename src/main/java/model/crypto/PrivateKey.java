package model.crypto;

import model.Entity;

/**
 * Represents abstract data type for the cryptographic private key used in LightChain.
 */
public abstract class PrivateKey {
  protected final byte[] bytes;

  public PrivateKey(byte[] bytes) {
    this.bytes = bytes.clone();
  }

  /**
   * Signs the given entity using private key.
   *
   * @param e entity to sign.
   * @return a signature over entity e using private key.
   */
  public abstract Signature signEntity(Entity e);
}