package crypto;

import model.crypto.Hash;
import model.crypto.Sha3256Hash;
import model.lightchain.Identifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;


public class Sha3256HashTest {

  private static final Random rand = new Random();

  /**
   * Test if the hashing of a random string is equal to the hashing of the same string
   */
  @Test
  public void TestHashingSameThingTwice() {
    byte[] bytesString = new byte[32];
    rand.nextBytes(bytesString);
    Sha3256Hash hash1 = new Sha3256Hash(bytesString);
    Sha3256Hash hash2 = new Sha3256Hash(bytesString);
    Assertions.assertEquals(hash1.compare(hash2), Hash.EQUAL);
  }

  /**
   * Test if the hashing of two different strings with one is less than the other
   */
  @Test
  public void TestHashingLess() {
    String testHex1 = "e167f68d6563d75bb25f3aa49c29ef61";
    String testHex2 = "e167f68d6563d75bb25f3aa49c29ef62";
    Sha3256Hash hash1 = new Sha3256Hash(testHex1.getBytes().clone());
    Sha3256Hash hash2 = new Sha3256Hash(testHex2.getBytes().clone());
    Assertions.assertEquals(hash1.compare(hash2), Hash.LESS);
  }

  /**
   * Test if the hashing of two different strings with one is greater than the other
   */
  @Test
  public void TestHashingGreater() {
    String testHex1 = "e167f68d6563d75bb25f3aa49c29ef61";
    String testHex2 = "e167f68d6563d75bb25f3aa49c29ef60";
    Sha3256Hash hash1 = new Sha3256Hash(testHex1.getBytes().clone());
    Sha3256Hash hash2 = new Sha3256Hash(testHex2.getBytes().clone());
    Assertions.assertEquals(hash1.compare(hash2), Hash.GREATER);
  }

  /**
   * Test if two constructors of Sha3256Hash output the same hash
   */
  @Test
  public void TestHashingIdentifier() {
    String testHex = "e167f68d6563d75bb25f3aa49c29ef61";
    Identifier identifier1 = new Identifier(testHex.getBytes().clone());
    Sha3256Hash hash = new Sha3256Hash(identifier1.getBytes().clone());
    Identifier identifier2 = hash.toIdentifier();
    Assertions.assertEquals(identifier1.comparedTo(identifier2), Hash.EQUAL);
  }
}
