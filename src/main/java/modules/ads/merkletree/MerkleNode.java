package modules.ads.merkletree;

import crypto.Sha3256Hasher;
import model.Entity;
import model.crypto.Sha3256Hash;

public class MerkleNode {
  private static final Sha3256Hasher hasher = new Sha3256Hasher();
  private MerkleNode left;
  private MerkleNode right;
  private MerkleNode parent;
  private boolean isLeft;
  private Sha3256Hash hash;

  public MerkleNode() {
    this.left = null;
    this.right = null;
    this.parent = null;
    this.isLeft = false;
    this.hash = null;
  }

  public MerkleNode(Entity e, boolean isLeft) {
    this.left = null;
    this.right = null;
    this.parent = null;
    this.isLeft = isLeft;
    this.hash = hasher.computeHash(e.id());
  }

  public MerkleNode(Sha3256Hash hash) {
    this.left = null;
    this.right = null;
    this.parent = null;
    this.isLeft = false;
    this.hash = hash;
  }

  public MerkleNode(Sha3256Hash hash, MerkleNode left, MerkleNode right) {
    this.left = left;
    this.right = right;
    this.parent = null;
    this.isLeft = false;
    this.hash = hash;
  }

  public MerkleNode getLeft() {
    return left;
  }

  public void setLeft(MerkleNode left) {
    this.left = left;
  }

  public MerkleNode getRight() {
    return right;
  }

  public void setRight(MerkleNode right) {
    this.right = right;
  }

  public MerkleNode getParent() {
    return parent;
  }

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

  public void setLeft(boolean isLeft) {
    this.isLeft = isLeft;
  }

  public MerkleNode getSibling() {
    if (isLeft()) {
      return parent.getRight();
    } else {
      return parent.getLeft();
    }
  }

}
