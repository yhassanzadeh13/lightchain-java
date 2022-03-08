package model.crypto.ecdsa;

import model.Entity;
import model.crypto.PublicKey;
import model.crypto.Signature;

public class EcdsaPublicKey extends PublicKey {
  public EcdsaPublicKey(byte[] bytes) {
    super(bytes);
  }

  /**
   * Implements signature verification.
   *
   * @param e entity that carries a signature on.
   * @param s digital signature over the entity.
   * @return true if s carries a valid signature over e against this public key, false otherwise.
   */
  @Override
  public boolean verifySignature(Entity e, Signature s) {
    return false;
  }
}
