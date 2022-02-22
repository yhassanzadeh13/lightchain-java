package model.crypto;

import model.Entity;

public abstract class PublicKey {
  private final byte[] bytes;

  public PublicKey(byte[] bytes) {
    this.bytes = bytes;
  }

  public abstract boolean VerifySignature(Entity e, Signature s);
}
