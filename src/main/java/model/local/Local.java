package model.local;

import edu.umd.cs.findbugs.util.NotImplementedYetException;
import model.Entity;
import model.crypto.Signature;
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
    throw new NotImplementedYetException("method not implemented yet");
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
