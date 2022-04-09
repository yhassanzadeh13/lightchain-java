package network.p2p.Fixtures;

import com.google.protobuf.ByteString;
import model.Entity;
import model.crypto.Sha3256Hash;
import model.lightchain.Identifier;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

/**
 * Encapsulates test utilities for a LightChain entity.
 */
public class EntityFixture extends Entity {
  private static final String TYPE_FIXTURE_ENTITY = "fixture-entity-type";
  private static final Random rand = new Random();
  public Identifier id;
  private final int testInt;
  private final String testString;
  private final double testDouble;
  private final byte[] testBytes;
  private final Sha3256Hash[] testHashArray;

  /**
   * Creates a new randomly looking lightchain entity.
   */
  public EntityFixture() {
    super();
    this.id = IdentifierFixture.newIdentifier();
    this.testInt = rand.nextInt();
    byte[] bytesString = new byte[32];
    rand.nextBytes(bytesString);
    this.testString = new String(bytesString);
    this.testDouble = rand.nextDouble();
    this.testBytes = Bytes.byteArrayFixture(32).clone();
    this.testHashArray = Sha3256HashFixture.newSha3256HashArray();
  }

  public EntityFixture(Object o) {
    super();
    this.id = (Identifier) o;
    this.testInt = rand.nextInt();
    byte[] bytesString = new byte[32];
    rand.nextBytes(bytesString);
    this.testString = new String(bytesString);
    this.testDouble = rand.nextDouble();
    this.testBytes = Bytes.byteArrayFixture(32).clone();
    this.testHashArray = Sha3256HashFixture.newSha3256HashArray();
  }

  @Override
  public String type() {
    return TYPE_FIXTURE_ENTITY;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EntityFixture that = (EntityFixture) o;
    return testInt == that.testInt && Double.compare(that.testDouble, testDouble) == 0
        && id.comparedTo(that.id) == 0 && testString.equals(that.testString)
        && Arrays.equals(testBytes, that.testBytes) && Arrays.equals(testHashArray, that.testHashArray);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(id, testInt, testString, testDouble);
    result = 31 * result + Arrays.hashCode(testBytes);
    result = 31 * result + Arrays.hashCode(testHashArray);
    return result;
  }
}
