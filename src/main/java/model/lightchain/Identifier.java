package model.lightchain;

/**
 * Represents a 32-byte unique identifier for an entity. Normally is computed as the hash value of the entity.
 */
public class Identifier {
  public static final int Size = 32;
  private final byte[] value;

  public Identifier(byte[] value) {
    this.value = value;
  }

  public byte[] getBytes() {
    return this.value;
  }
}
