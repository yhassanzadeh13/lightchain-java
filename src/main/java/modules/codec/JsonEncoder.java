package modules.codec;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
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
    Gson gson = new Gson();
    byte[] bytes = gson.toJson(e).getBytes(StandardCharsets.UTF_8);
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
    Gson gson = new Gson();
    String json = new String(e.getBytes().clone(), StandardCharsets.UTF_8);
    return gson.fromJson(json, (Type) Class.forName(e.getType()));

  }
}
