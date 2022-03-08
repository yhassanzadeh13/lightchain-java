package unittest.fixtures;

import model.Entity;
import model.crypto.PrivateKey;
import model.crypto.Signature;

public class PrivateKeyFixture extends PrivateKey {
  public PrivateKeyFixture(byte[] bytes) {
    super(bytes);
  }

  @Override
  public Signature signEntity(Entity e) {
    return null;
  }
}
