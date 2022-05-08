package modules.ads.mtrie;

import model.Entity;
import model.crypto.Sha3256Hash;
import model.lightchain.Identifier;

public class MerklePatriciaLeafNode extends MerklePatriciaNode {
  private Sha3256Hash hash;

  public MerklePatriciaLeafNode(Identifier id) {
    this.hash = new Sha3256Hash(id);
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

}
