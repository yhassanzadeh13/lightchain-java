package unittest.fixtures;


import model.crypto.Sha3256Hash;

public class Sha3256HashFixture {
  public static model.crypto.Sha3256Hash NewSha3256Hash() {
    byte[] bytes = Bytes.ByteArrayFixture(model.crypto.Sha3256Hash.Size);
    return new model.crypto.Sha3256Hash(bytes);
  }

  public static model.crypto.Sha3256Hash[] NewSha3256HashArray() {
    Sha3256Hash[] hashArray = new Sha3256Hash[32];
    for (int i = 0; i < 32; i++) {
      hashArray[i] = NewSha3256Hash();
    }
    return hashArray;
  }
}
