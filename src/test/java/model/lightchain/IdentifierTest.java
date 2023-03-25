package model.lightchain;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.ipfs.multibase.Multibase;
import org.junit.jupiter.api.Test;

class IdentifierTest {
  private static final Random random = new Random();
  @Test
  void testIdentifierConstructorWithByteArray() {
    byte[] value = new byte[Identifier.Size];
    random.nextBytes(value);

    Identifier identifier = new Identifier(value);

    assertArrayEquals(value, identifier.getBytes());
  }

  @Test
  void testIdentifierConstructorWithInvalidByteArraySize() {
    byte[] value = new byte[Identifier.Size + 1];
    random.nextBytes(value);

    assertThrows(IllegalArgumentException.class, () -> new Identifier(value));
  }

  @Test
  void testIdentifierConstructorWithString() {
    byte[] value = new byte[Identifier.Size];
    random.nextBytes(value);

    String identifierString = Multibase.encode(Multibase.Base.Base58BTC, value);

    Identifier identifier = new Identifier(identifierString);

    assertEquals(identifierString, identifier.toString());
  }

  @Test
  void testIdentifierConstructorWithInvalidString() {
    String invalidIdentifierString = "invalid identifier string";

    assertThrows(RuntimeException.class, () -> new Identifier(invalidIdentifierString));
  }

  @Test
  void testEqualsAndHashCode() {
    byte[] value1 = new byte[Identifier.Size];
    random.nextBytes(value1);
    Identifier identifier1 = new Identifier(value1);

    byte[] value2 = value1.clone();
    Identifier identifier2 = new Identifier(value2);

    assertTrue(identifier1.equals(identifier2) && identifier2.equals(identifier1));
    assertEquals(identifier1.hashCode(), identifier2.hashCode());
  }

  @Test
  void testCompareTo() {
    byte[] value1 = new byte[Identifier.Size];
    random.nextBytes(value1);
    Arrays.fill(value1, (byte) 1);
    Identifier identifier1 = new Identifier(value1);

    byte[] value2 = new byte[Identifier.Size];
    Arrays.fill(value2, (byte) 2);
    Identifier identifier2 = new Identifier(value2);

    assertEquals(0, identifier1.comparedTo(identifier1));
    assertTrue(identifier1.comparedTo(identifier2) < 0);
    assertTrue(identifier2.comparedTo(identifier1) > 0);
  }

  @Test
  void testRoundTrip() {
    byte[] value = new byte[Identifier.Size];
    random.nextBytes(value);

    Identifier identifier = new Identifier(value);
    Identifier identifier2 = new Identifier(identifier.toString());
    assertEquals(identifier, identifier2);
    assertEquals(identifier.toString(), identifier2.toString());
  }
}
