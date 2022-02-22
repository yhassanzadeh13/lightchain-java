package model.crypto;

public abstract class Signature {
  private final byte[] bytes;

  protected Signature(byte[] bytes) {
    this.bytes = bytes;
  }
}
