package modules;

import model.Entity;
import model.exceptions.CodecException;
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
  public void testEncodingRoundTrip() throws CodecException {
    JsonEncoder encoder = new JsonEncoder();
    EntityFixture entity = new EntityFixture();
    Entity entityChanged = encoder.decode(encoder.encode(entity));
    Assertions.assertEquals(entity, entityChanged);
  }
}
