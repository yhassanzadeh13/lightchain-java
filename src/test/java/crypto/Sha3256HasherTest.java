package crypto;

import model.codec.EncodedEntity;
import model.crypto.Hash;
import model.fixture.TestEntity;
import model.lightchain.Identifier;
import modules.codec.JsonEncoder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.apache.commons.codec.binary.Hex;

public class Sha3256HasherTest {

    // length test of the hash
    @Test
    public void Test1() {
        TestEntity testEntity = new TestEntity(2022, "Hello World!", 3.14, "test type");
        JsonEncoder encoder = new JsonEncoder();
        EncodedEntity encodedEntity = encoder.encode(testEntity);
        Sha3256Hasher hasher = new Sha3256Hasher();
        Hash hash = hasher.computeHash(encodedEntity);
        Identifier identifier = hash.toIdentifier();
        byte[] bytes = identifier.getBytes();
        Assertions.assertEquals(32, bytes.length);
    }

    // testing of the same entity for 100 times to check if they are the same
    @RepeatedTest(100)
    public void Test2() {
        String hashHex = "02a1b9e8174477e593adb5f8b6bcb1a98a4c6dc908bc7e5c406812a7f2fc0629"; // ran test manually and got this hash
        TestEntity testEntity = new TestEntity(0, "test String", 3.1415, "test");
        JsonEncoder encoder = new JsonEncoder();
        EncodedEntity encodedEntity = encoder.encode(testEntity);
        Sha3256Hasher hasher = new Sha3256Hasher();
        Hash hash = hasher.computeHash(encodedEntity);
        Identifier identifier = hash.toIdentifier();
        byte[] bytes = identifier.getBytes();
        Assertions.assertEquals(Hex.encodeHexString(bytes), hashHex);
    }

    // testing of the different entities for 100 times to check if they are NOT the same
    @RepeatedTest(100)
    public void Test3() {
        TestEntity testEntity1 = new TestEntity(1, "test String1", 3.1, "test1");
        TestEntity testEntity2 = new TestEntity(2, "test String2", 3.141, "test2");
        JsonEncoder encoder = new JsonEncoder();
        EncodedEntity encodedEntity1 = encoder.encode(testEntity1);
        EncodedEntity encodedEntity2 = encoder.encode(testEntity2);
        Sha3256Hasher hasher = new Sha3256Hasher();
        Hash hash1 = hasher.computeHash(encodedEntity1);
        Hash hash2 = hasher.computeHash(encodedEntity2);
        Identifier identifier1 = hash1.toIdentifier();
        Identifier identifier2 = hash2.toIdentifier();
        byte[] bytes1 = identifier1.getBytes();
        byte[] bytes2 = identifier2.getBytes();
        Assertions.assertNotEquals(Hex.encodeHexString(bytes1), Hex.encodeHexString(bytes2));
    }
}
