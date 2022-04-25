package modules.codec;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import model.Entity;
import model.codec.EncodedEntity;

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
  public EncodedEntity encode(Entity e) {
    byte[] bytes = new byte[0];
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream out = null;
      out = new ObjectOutputStream(bos);
      out.writeObject(e);
      out.flush();
      bytes = bos.toByteArray();
    } catch (IOException ex) {
      ex.printStackTrace();
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
  public Entity decode(EncodedEntity e) throws ClassNotFoundException {
    Entity entity = null;
    try {
      ByteArrayInputStream bis = new ByteArrayInputStream(e.getBytes().clone());
      ObjectInputStream inp = null;
      inp = new ObjectInputStream(bis);
      entity = (Entity) (Class.forName(e.getType())).cast(inp.readObject());
    } catch (IOException ex) {
      ex.printStackTrace();
    } catch (ClassNotFoundException ex) {
      throw new RuntimeException(ex);
    }
    return entity;
  }
}
