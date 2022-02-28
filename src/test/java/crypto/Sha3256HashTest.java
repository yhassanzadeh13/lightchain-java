package crypto;

import model.crypto.Sha3256Hash;
import model.lightchain.Identifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Sha3256HashTest {

    // compare tests
    @Test
    public void Test1(){
        String testHex = "e167f68d6563d75bb25f3aa49c29ef612d41352dc00606de7cbd630bb2665f51"; // SHA3-256 of "Hello World"
        Sha3256Hash hash = new Sha3256Hash(testHex.getBytes());
        Assertions.assertEquals(hash.compare(new Sha3256Hash(testHex.getBytes())), 0);
    }

    @Test
    public void Test2(){
        String testHex1 = "e167f68d6563d75bb25f3aa49c29ef612d41352dc00606de7cbd630bb2665f51"; // SHA3-256 of "Hello World"
        String testHex2 = "e167f68d6563d75bb25f3aa49c29ef612d41352dc00606de7cbd630bb2665f52"; // SHA3-256 of "Hello World" + 1
        Sha3256Hash hash = new Sha3256Hash(testHex1.getBytes());
        Assertions.assertEquals(hash.compare(new Sha3256Hash(testHex2.getBytes())), -1);
    }

    @Test
    public void Test3(){
        String testHex1 = "e167f68d6563d75bb25f3aa49c29ef612d41352dc00606de7cbd630bb2665f51"; // SHA3-256 of "Hello World"
        String testHex2 = "e167f68d6563d75bb25f3aa49c29ef612d41352dc00606de7cbd630bb2665f50"; // SHA3-256 of "Hello World" - 1
        Sha3256Hash hash = new Sha3256Hash(testHex1.getBytes());
        Assertions.assertEquals(hash.compare(new Sha3256Hash(testHex2.getBytes())), 1);
    }

    // toIdentifier test
    @Test
    public void Test4(){
        String testHex1 = "e167f68d6563d75bb25f3aa49c29ef612d41352dc00606de7cbd630bb2665f51"; // SHA3-256 of "Hello World"
        Identifier identifier = new Identifier(testHex1.getBytes());
        Sha3256Hash hash = new Sha3256Hash(identifier.getBytes());
        Identifier identifierFromHash = hash.toIdentifier();
        Assertions.assertEquals(identifier.comparedTo(identifierFromHash), 0);
    }
}
