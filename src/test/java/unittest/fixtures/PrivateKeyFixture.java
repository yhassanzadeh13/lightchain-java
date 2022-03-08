package unittest.fixtures;


import model.crypto.ecdsa.EcdsaPrivateKey;

public class PrivateKeyFixture extends EcdsaPrivateKey {
  private final byte[] bytes;

  public PrivateKeyFixture(byte[] bytes) {
    super(bytes);
    this.bytes = bytes.clone();
  }

}
