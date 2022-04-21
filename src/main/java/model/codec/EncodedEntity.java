package model.codec;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Represents an encapsulation around the byte representation of an entity accompanied by its original type.
 */
public class EncodedEntity implements Serializable {
  private final byte[] bytes;
  private final String type;



  // EncodedEntity(id.getBytes() || byte(i), "assignment")
  public EncodedEntity(byte[] bytes, String type) {
    this.bytes = bytes.clone();
    this.type = type;
  }
  /**
   * Hashcode of entity.
   *
   * @return hashcode of encodedentity.
   */
  @Override
  public int hashCode() {
    return Arrays.hashCode(this.bytes);
  }

  /**
   * Check if objects are equal
   *
   * @param o encodedentity.
   * @return true if equals.
   */
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof EncodedEntity)) {
      return false;
    }
    EncodedEntity that = (EncodedEntity) o;
    return Arrays.equals(this.bytes, that.bytes);
  }
  public byte[] getBytes() {
    return bytes.clone();
  }

  public String getType() {
    return type;
  }
}