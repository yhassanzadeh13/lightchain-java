package modules.codec;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.Entity;
import model.codec.EncodedEntity;

import java.io.IOException;

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
      byte[] bytes = mapper.writeValueAsString(e).getBytes();
      String type = e.type();
      return new EncodedEntity(bytes, type);
    }
    catch (JsonProcessingException ex) {
      ex.printStackTrace();
    }
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
    try {
      ObjectMapper mapper = new ObjectMapper();
      String json = new String(e.getBytes());
      return mapper.readValue(json, Entity.class);
    }
    catch (IOException ex) {
      ex.printStackTrace();
    }
    return null;
  }
}
