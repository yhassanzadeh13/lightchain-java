package modules.ads.merkletree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import model.crypto.Sha3256Hash;
import modules.ads.MembershipProof;

/**
 * A proof of membership in a Merkle tree.
 */
public class Proof implements MembershipProof {
  private ArrayList<Sha3256Hash> path;
  private Sha3256Hash root;

  public Proof(ArrayList<Sha3256Hash> path, Sha3256Hash root) {
    this.path = new ArrayList<>(path);
    this.root = root;
  }

  @Override
  public ArrayList<Sha3256Hash> getPath() {
    return new ArrayList<>(path);
  }

  public void setPath(ArrayList<Sha3256Hash> path) {
    this.path = new ArrayList<>(path);
  }

  public Sha3256Hash getRoot() {
    return root;
  }

  @Override
  public void setRoot(Sha3256Hash root) {
    this.root = root;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Proof proof = (Proof) o;
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
