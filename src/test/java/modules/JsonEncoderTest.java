package modules;

import model.Entity;
import model.fixture.TestEntity;
import modules.codec.JsonEncoder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JsonEncoderTest {

  @Test
  public void Test() {
    JsonEncoder encoder = new JsonEncoder();
    TestEntity entity = new TestEntity(16482, "dummy string", 3.14159, "TestEntity");
    Entity entityChanged = encoder.decode(encoder.encode(entity));
    Assertions.assertEquals(entity, entityChanged);
    System.out.println("Entities are equal: " + entity.equals(entityChanged));
  }
}
