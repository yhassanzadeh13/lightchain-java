package modules.ads.mtrie;

import model.crypto.Sha3256Hash;

public abstract class MerklePatriciaNode {
  private MerklePatriciaNode parent;

  /**
   * Updates the hash of the merkle patricia node.
   */
  public abstract void updateHash();

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

  /**
   * Returns the parent of this merkle patricia node.
   *
   * @return the parent of this merkle patricia node.
   */
  public abstract MerklePatriciaNode getParent();
}
