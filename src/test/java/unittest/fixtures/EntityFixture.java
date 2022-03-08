package unittest.fixtures;

import model.Entity;
import model.crypto.Sha3256Hash;
import model.lightchain.Identifier;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

public class EntityFixture extends Entity {
  private static final String TYPE_FIXTURE_ENTITY = "fixture-entity-type";
  private static final Random rand = new Random();
  private final Identifier id;
  private final int testInt;
  private final String testString;
  private final double testDouble;
  private final byte[] testBytes;
  private final Sha3256Hash[] testHashArray;

  public EntityFixture() {
    super();
    this.id = IdentifierFixture.NewIdentifier();
    this.testInt = rand.nextInt();
    byte[] bytesString = new byte[32];
    rand.nextBytes(bytesString);
    this.testString = new String(bytesString);
    this.testDouble = rand.nextDouble();
    this.testBytes = Bytes.ByteArrayFixture(32).clone();
    this.testHashArray = Sha3256HashFixture.NewSha3256HashArray();
  }


  @Override
  public String type() {
    return TYPE_FIXTURE_ENTITY;
  }

  @Override
  public Identifier id() {
    return this.id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
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
