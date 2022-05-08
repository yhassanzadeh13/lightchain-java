package modules.ads.mtrie;

import model.crypto.Sha3256Hash;

public abstract class MerklePatriciaNode {

  /**
   * Hash of this merkle patricia node.
   *
   * @return hash of this merkle patricia node.
   */
  public abstract Sha3256Hash getHash();

  /**
   * Type of this merkle patricia node.
   *
   * @return type of this entity.
   */
  public abstract String type();
}
