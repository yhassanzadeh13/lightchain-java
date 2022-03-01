package modules.codec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    try {
      ObjectMapper mapper = new ObjectMapper();
      byte[] bytes = mapper.writeValueAsString(e).getBytes(StandardCharsets.UTF_8);
      String type = e.getClass().getCanonicalName();
      return new EncodedEntity(bytes, type);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to encode Entity to JSON", ex);
    }
  }

  /**
   * Decodes a JSON EncodedEntity to its original Entity type.
   *
   * @param e input JSON EncodedEntity.
   * @return original Entity type.
   */
  @Override
  public Entity decode(EncodedEntity e) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      String json = new String(e.getBytes(), StandardCharsets.UTF_8);
      JavaType a = mapper.getTypeFactory().constructFromCanonical(e.getType());
      return mapper.readValue(json, a);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to encode Entity to JSON", ex);
    }
  }
}
