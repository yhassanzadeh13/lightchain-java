package unittest.fixtures;

import model.Entity;
import model.crypto.PublicKey;
import model.crypto.Signature;

public class PublicKeyFixture extends PublicKey {
  public PublicKeyFixture(byte[] bytes) {
    super(bytes);
  }

  @Override
  public boolean verifySignature(Entity e, Signature s) {
    return false;
  }
}
