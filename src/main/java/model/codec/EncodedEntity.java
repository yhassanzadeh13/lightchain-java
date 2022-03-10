package model.codec;

/**
 * Represents an encapsulation around the byte representation of an entity accompanied by its original type.
 */
public class EncodedEntity {
  private final byte[] bytes;
  private final String type;

  // EncodedEntity(id.getBytes() || byte(i), "assignment")
  public EncodedEntity(byte[] bytes, String type) {
    this.bytes = bytes.clone();
    this.type = type;
  }

  public byte[] getBytes() {
    return bytes.clone();
  }

  public String getType() {
    return type;
  }
}