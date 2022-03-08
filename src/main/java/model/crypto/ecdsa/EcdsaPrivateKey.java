package model.crypto.ecdsa;

import model.Entity;
import model.crypto.PrivateKey;
import model.crypto.Signature;

public class EcdsaPrivateKey extends PrivateKey {
  public EcdsaPrivateKey(byte[] bytes) {
    super(bytes);
  }

  /**
   * Signs the given entity using private key.
   *
   * @param e entity to sign.
   * @return a signature over entity e using private key.
   */
  @Override
  public Signature signEntity(Entity e) {
    return null;
  }
}
