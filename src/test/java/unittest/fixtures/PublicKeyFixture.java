package unittest.fixtures;


import model.crypto.ecdsa.EcdsaPublicKey;

public class PublicKeyFixture extends EcdsaPublicKey {
  private final byte[] bytes;

  public PublicKeyFixture(byte[] bytes) {
    super(bytes);
    this.bytes = bytes.clone();
  }

}
