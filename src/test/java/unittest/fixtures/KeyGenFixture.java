package unittest.fixtures;

import model.crypto.ecdsa.EcdsaKeyGen;

public class KeyGenFixture extends EcdsaKeyGen {
  public static model.crypto.ecdsa.EcdsaKeyGen NewKeyGen() {
    return new model.crypto.ecdsa.EcdsaKeyGen();
  }
}
