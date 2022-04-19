package modules.ads.merkletree;

import crypto.Sha3256Hasher;
import model.Entity;
import model.crypto.Sha3256Hash;

/**
 * A node in the Merkle tree.
 */
public class MerkleNode {
  private static final Sha3256Hasher hasher = new Sha3256Hasher();
  private MerkleNode left;
  private MerkleNode right;
  private MerkleNode parent;
  private boolean isLeft;
  private Sha3256Hash hash;

  /**
   * Default constructor.
   */
  public MerkleNode() {
    this.left = null;
    this.right = null;
    this.parent = null;
    this.isLeft = false;
    this.hash = null;
  }

  /**
   * Constructor with entity and isLeft.
   *
   * @param e      input entity
   * @param isLeft boolean that specifies if the node is left child or not
   */
  public MerkleNode(Entity e, boolean isLeft) {
    this.left = null;
    this.right = null;
    this.parent = null;
    this.isLeft = isLeft;
    this.hash = hasher.computeHash(e.id());
  }

  /**
   * Constructor with hash of the entity.
   *
   * @param hash input hash of the entity corresponding to that node
   */
  public MerkleNode(Sha3256Hash hash) {
    this.left = null;
    this.right = null;
    this.parent = null;
    this.isLeft = false;
    this.hash = hash;
  }

  /**
   * Constructor of a parent node.
   *
   * @param hash input hash of the entity corresponding to that node
   * @param left left child of the node
   * @param right right child of the node
   */
  @SuppressWarnings("EI_EXPOSE_REP2")
  public MerkleNode(Sha3256Hash hash, MerkleNode left, MerkleNode right) {
    this.left = left;
    this.right = right;
    this.parent = null;
    this.isLeft = false;
    this.hash = hash;
  }

  @SuppressWarnings("EI_EXPOSE_REP")
  public MerkleNode getLeft() {
    return left;
  }

  @SuppressWarnings("EI_EXPOSE_REP")
  public MerkleNode getRight() {
    return right;
  }

  public void setRight(MerkleNode right) {
    this.right = right;
  }

  @SuppressWarnings("EI_EXPOSE_REP")
  public MerkleNode getParent() {
    return parent;
  }

  @SuppressWarnings("EI_EXPOSE_REP2")
  public void setParent(MerkleNode parent) {
    this.parent = parent;
  }

  public Sha3256Hash getHash() {
    return hash;
  }

  public void setHash(Sha3256Hash hash) {
    this.hash = hash;
  }

  public boolean isLeft() {
    return isLeft;
  }

  @SuppressWarnings("EI_EXPOSE_REP2")
  public void setLeft(MerkleNode left) {
    this.left = left;
  }

  @SuppressWarnings("EI_EXPOSE_REP2")
  public void setLeft(boolean isLeft) {
    this.isLeft = isLeft;
  }

  /**
   * Returns the sibling of the node.
   *
   * @return the sibling of the node
   */
  public MerkleNode getSibling() {
    if (isLeft()) {
      return parent.getRight();
    } else {
      return parent.getLeft();
    }
  }

}
