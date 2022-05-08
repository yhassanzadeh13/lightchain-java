package modules.ads.mtrie;

import crypto.Sha3256Hasher;
import model.crypto.Sha3256Hash;
import model.lightchain.Identifier;

public class MerklePatriciaBranchNode extends MerklePatriciaNode {
  private final MerklePatriciaNode[] children = new MerklePatriciaNode[256];
  private final Sha3256Hasher hasher = new Sha3256Hasher();

  /**
   * Creates a child.
   *
   * @param index index of the child.
   * @param child child to create.
   * @return created child.
   */
  public MerklePatriciaNode createChild(int index, MerklePatriciaNode child) {
    children[index] = child;
    return child;
  }

  /**
   * Creates a leaf node with the given identifier.
   *
   * @param idx index of the leaf node.
   * @param id  identifier of the leaf node.
   * @return created leaf node.
   */
  public MerklePatriciaNode createLeaf(int idx, Identifier id) {
    return createChild(idx, new MerklePatriciaLeafNode(id));
  }

  /**
   * Get the child node with the given index.
   *
   * @param idx index of the child node.
   * @return child node with the given index.
   */
  public MerklePatriciaBranchNode getChild(int idx) {
    if (children[idx] == null) {
      createChild(idx, new MerklePatriciaBranchNode());
    }
    return (MerklePatriciaBranchNode) children[idx];
  }


  /**
   * Hash of this merkle patricia node.
   *
   * @return hash of this merkle patricia node.
   */
  @Override
  public Sha3256Hash getHash() {
    Sha3256Hash[] hashes = new Sha3256Hash[256];
    for (int i = 0; i < 256; i++) {
      if (children[i] != null) {
        hashes[i] = children[i].getHash();
      } else {
        hashes[i] = new Sha3256Hash(new byte[32]);
      }
    }
    return hasher.computeHash(hashes);
  }

  /**
   * Type of this merkle patricia node.
   *
   * @return type of this entity.
   */
  @Override
  public String type() {
    return MerklePatriciaNodeType.TYPE_BRANCH;
  }

  public void setLeaf(int idx, Identifier id) {
  }
}
