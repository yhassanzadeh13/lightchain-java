package modules.codec;

import model.Entity;
import model.codec.EncodedEntity;

/**
 * Implements encoding and decoding using JSON.
 */
public class JsonEncoder implements Codec {
  /**
   * Encodes an Entity to an EncodedEntity.
   *
   * @param e input Entity.
   * @return the JSON encoded representation of Entity.
   */
  @Override
  public EncodedEntity encode(Entity e) {
    return null;
  }

  /**
   * Decodes a JSON EncodedEntity to its original Entity type.
   *
   * @param e input JSON EncodedEntity.
   * @return original Entity type.
   */
  @Override
  public Entity decode(EncodedEntity e) {
    return null;
  }
}
