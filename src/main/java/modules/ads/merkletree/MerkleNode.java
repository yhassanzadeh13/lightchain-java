package modules.ads.merkletree;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.google.gson.Gson;
import crypto.Sha3256Hasher;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import model.Entity;
import model.crypto.Sha3256Hash;

/**
 * A node in the Merkle tree.
 */
public class MerkleNode implements Serializable {
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
   * @param hash  input hash of the entity corresponding to that node
   * @param left  left child of the node
   * @param right right child of the node
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "left and right are intentionally mutable externally")
  public MerkleNode(Sha3256Hash hash, MerkleNode left, MerkleNode right) {
    this.left = left;
    this.right = right;
    this.parent = null;
    this.isLeft = false;
    this.hash = hash;
  }

  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "internal representation is intentionally returned")
  public MerkleNode getLeft() {
    return left;
  }

  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "internal representation is intentionally returned")
  public MerkleNode getRight() {
    return right;
  }

  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "internal representation is intentionally returned")
  public MerkleNode getParent() {
    return parent;
  }

  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "parent is intentionally mutable externally")
  public void setParent(MerkleNode parent) {
    this.parent = parent;
  }

  public Sha3256Hash getHash() {
    return hash;
  }

  public boolean isLeft() {
    return isLeft;
  }

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MerkleNode that = (MerkleNode) o;
    return hash.equals(that.hash);
  }

  @Override
  public int hashCode() {
    return Objects.hash(left, right, parent, isLeft, hash);
  }

  public byte[] getBytes() {
    Gson gson = new Gson();
    byte[] bytes = gson.toJson(this).getBytes(StandardCharsets.UTF_8);
    return bytes;
  }
}
