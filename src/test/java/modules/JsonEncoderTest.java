package modules;

import model.Entity;
import modules.codec.JsonEncoder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unittest.fixtures.EntityFixture;

public class JsonEncoderTest {

  /**
   * Round trip test of Json encoding and decoding.
   */
  @Test
  public void TestEncodingRoundTrip() throws ClassNotFoundException {
    JsonEncoder encoder = new JsonEncoder();
    EntityFixture entity = new EntityFixture();
    Entity entityChanged = encoder.decode(encoder.encode(entity));
    Assertions.assertEquals(entity, entityChanged);
    System.out.println("Entities are equal: " + entity.equals(entityChanged));
  }
}
