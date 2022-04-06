package model.local;

import edu.umd.cs.findbugs.util.NotImplementedYetException;
import model.Entity;
import model.crypto.PrivateKey;
import model.crypto.Signature;
import model.crypto.ecdsa.EcdsaKeyGen;
import model.crypto.ecdsa.EcdsaSignature;
import model.lightchain.Block;
import model.lightchain.Identifier;

/**
 * Local represents the set of utilities available to the current LightChain node.
 */
public class Local {
  /**
   * Signs the given entity using private key.
   *
   * @param e entity to sign.
   * @return a signature over entity e using private key.
   */
  public Signature signEntity(Entity e) throws IllegalStateException {
    PrivateKey pk = new EcdsaKeyGen().getPrivateKey();
    Signature sign = new EcdsaSignature(pk.signEntity(e).getBytes(), e.id());
    return sign;
  }

  /**
   * Returns identifier of the current node.
   *
   * @return identifier of the current node.
   */
  public Identifier myId() {
    throw new NotImplementedYetException("method not implemented yet");
  }
}
