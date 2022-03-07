package modules.codec;

import model.Entity;
import model.codec.EncodedEntity;

/**
 * Encapsulates the representation of encoding and decoding functionalities around the same protocol, e.g., JSON.
 */
public interface Codec {
  /**
   * Encodes an Entity to an EncodedEntity.
   *
   * @param e input Entity.
   * @return the encoded representation of Entity.
   */
  EncodedEntity encode(Entity e);

  /**
   * Decodes an EncodedEntity to its original Entity type.
   *
   * @param e input EncodedEntity.
   * @return original Entity type.
   */
  Entity decode(EncodedEntity e) throws ClassNotFoundException;
}
