package modules.codec;

import java.io.*;

import model.Entity;
import model.codec.EncodedEntity;
import model.exceptions.CodecException;

/**
 * Implements encoding and decoding using JSON.
 */
public class JsonEncoder implements Codec, Serializable {
  /**
   * Encodes an Entity to an EncodedEntity.
   *
   * @param e input Entity.
   * @return the JSON encoded representation of Entity.
   */
  @Override
  public EncodedEntity encode(Entity e) throws CodecException {
    byte[] bytes;
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream out = new ObjectOutputStream(bos);
      out.writeObject(e);
      out.flush();
      bytes = bos.toByteArray();
    } catch (IOException ex) {
      throw new CodecException("could not encode entity", ex);
    }
    String type = e.getClass().getCanonicalName();
    return new EncodedEntity(bytes, type);
  }

  /**
   * Decodes a JSON EncodedEntity to its original Entity type.
   *
   * @param e input JSON EncodedEntity.
   * @return original Entity type.
   */
  @Override
  public Entity decode(EncodedEntity e) throws CodecException {
    Entity entity = null;
    try {
      ByteArrayInputStream bis = new ByteArrayInputStream(e.getBytes().clone());
      ObjectInputStream inp = null;
      inp = new ObjectInputStream(bis);
      entity = (Entity) (Class.forName(e.getType())).cast(inp.readObject());
    } catch (IOException | ClassNotFoundException ex) {
      throw new CodecException("could not decode entity", ex);
    }
    return entity;
  }
}