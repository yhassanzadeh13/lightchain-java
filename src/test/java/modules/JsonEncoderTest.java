package modules;

import model.Entity;
import modules.codec.JsonEncoder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unittest.fixtures.EntityFixture;

/**
 * Tests json encoder.
 */
public class JsonEncoderTest {

  /**
   * Round trip test of Json encoding and decoding.
   */
  @Test
  public void testEncodingRoundTrip() throws ClassNotFoundException {
    JsonEncoder encoder = new JsonEncoder();
    Entity entity = new EntityFixture();
    Entity entityChanged = encoder.decode(encoder.encode(entity));
    Assertions.assertEquals(entity, entityChanged);
    System.out.println("Entities are equal: " + entity.equals(entityChanged));
  }
}
