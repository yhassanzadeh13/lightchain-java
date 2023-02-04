package model.local;

import model.Entity;
import model.crypto.PrivateKey;
import model.crypto.PublicKey;
import model.crypto.Signature;
import model.crypto.ecdsa.EcdsaSignature;
import model.lightchain.Identifier;

/**
 * Local represents the set of utilities available to the current LightChain node.
 */
public class Local {
  private final Identifier id;
  private final PrivateKey sk;
  private final PublicKey pk;

  /**
   * Constructor.
   *
   * @param id identifier of the node.
   * @param sk private key (i.e., secret key of the node).
   * @param pk public key of the node.
   */
  public Local(Identifier id, PrivateKey sk, PublicKey pk) {
    this.id = id;
    this.sk = sk;
    this.pk = pk;
  }

  /**
   * Signs the given entity using private key.
   *
   * @param e entity to sign.
   * @return a signature over entity e using private key.
   */
  public Signature signEntity(Entity e) throws IllegalStateException {
    return new EcdsaSignature(sk.signEntity(e).getBytes(), e.id());
  }

  /**
   * Returns identifier of the current node.
   *
   * @return identifier of the current node.
   */
  public Identifier myId() {
    return this.id;
  }

  public PublicKey myPublicKey() {
    return this.pk;
  }
}