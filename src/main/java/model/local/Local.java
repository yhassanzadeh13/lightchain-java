package model.local;

import model.Entity;
import model.crypto.PrivateKey;
import model.crypto.Signature;
import model.crypto.ecdsa.EcdsaSignature;
import model.lightchain.Identifier;

/**
 * Local represents the set of utilities available to the current LightChain node.
 */
public class Local {
  private final Identifier id;
  private PrivateKey pk;
  public Local(Identifier id, PrivateKey pk) {
    this.id = id;
    this.pk =pk;
  }

  /**
   * Signs the given entity using private key.
   *
   * @param e entity to sign.
   * @return a signature over entity e using private key.
   */
  public Signature signEntity(Entity e) throws IllegalStateException {
    Signature sign = new EcdsaSignature(pk.signEntity(e).getBytes(), e.id());
    return sign;
  }

  /**
   * Returns identifier of the current node.
   *
   * @return identifier of the current node.
   */
  public Identifier myId() {
    return this.id;
  }

  /**
   * Returns private key of the current node.
   *
   * @return private key of the current node.
   */
  public PrivateKey myPrivateKey() {
    return this.pk;
  }
}
