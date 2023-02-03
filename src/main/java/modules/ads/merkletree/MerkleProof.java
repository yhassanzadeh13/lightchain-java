package modules.ads.merkletree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import model.crypto.Sha3256Hash;
import modules.ads.MembershipProof;

/**
 * A proof of membership in a Merkle tree.
 */
public class MerkleProof implements MembershipProof, Serializable {
  private final ArrayList<Boolean> isLeftNode;
  private final Sha3256Hash root;
  private ArrayList<Sha3256Hash> path;

  /**
   * Constructs a proof from a list of hashes and a root.
   *
   * @param path       the list of hashes
   * @param root       the root
   * @param isLeftNode the list of isLeft Boolean values of the hashes
   */
  public MerkleProof(ArrayList<Sha3256Hash> path, Sha3256Hash root, ArrayList<Boolean> isLeftNode) {
    this.path = new ArrayList<>(path);
    this.root = root;
    this.isLeftNode = new ArrayList<>(isLeftNode);
  }

  @Override
  public ArrayList<Sha3256Hash> getPath() {
    return new ArrayList<>(path);
  }

  public void setPath(ArrayList<Sha3256Hash> path) {
    this.path = new ArrayList<>(path);
  }

  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "internal representation is intentionally returned")
  public ArrayList<Boolean> getIsLeftNode() {
    return isLeftNode;
  }

  public Sha3256Hash getRoot() {
    return root;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MerkleProof proof = (MerkleProof) o;
    for (int i = 0; i < path.size(); i++) {
      if (!Arrays.equals(path.get(i).getBytes(), proof.path.get(i).getBytes())) {
        return false;
      }
    }
    return root.equals(proof.root);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path, root);
  }
}
