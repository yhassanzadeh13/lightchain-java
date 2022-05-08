package modules.ads.mtrie;

import model.crypto.Sha3256Hash;
import model.lightchain.Identifier;

public class MerklePatriciaLeafNode extends MerklePatriciaNode {
  private MerklePatriciaNode parent;
  private Sha3256Hash hash;

  public MerklePatriciaLeafNode(Identifier id, MerklePatriciaNode parent) {
    this.hash = new Sha3256Hash(id);
    this.parent = parent;
  }

  /**
   * Updates the hash of the merkle patricia node.
   */
  @Override
  public void updateHash() {

  }

  /**
   * Hash of this merkle patricia node.
   *
   * @return hash of this merkle patricia node.
   */
  @Override
  public Sha3256Hash getHash() {
    return this.hash;
  }

  /**
   * Type of this merkle patricia node.
   *
   * @return type of this entity.
   */
  @Override
  public String type() {
    return MerklePatriciaNodeType.TYPE_LEAF;
  }

  /**
   * Returns the parent of this merkle patricia node.
   *
   * @return the parent of this merkle patricia node.
   */
  @Override
  public MerklePatriciaNode getParent() {
    return this.parent;
  }

}
