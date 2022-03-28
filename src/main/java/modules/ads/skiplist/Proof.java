package modules.ads.skiplist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import model.crypto.Sha3256Hash;
import model.lightchain.Identifier;
import modules.ads.MembershipProof;

public class Proof implements MembershipProof {
  private ArrayList<byte[]> path;
  private Sha3256Hash root;
  private boolean isPresent;

  public Proof(ArrayList<byte[]> path, Sha3256Hash root, boolean isPresent) {
    this.path = path;
    this.root = root;
    this.isPresent = isPresent;
  }

  @Override
  public ArrayList<byte[]> getPath() {
    return path;
  }

  public void setPath(ArrayList<byte[]> path) {
    this.path = path;
  }

  @Override
  public Sha3256Hash getRoot() {
    return root;
  }

  public void setRoot(Sha3256Hash root) {
    this.root = root;
  }

  public boolean isPresent() {
    return isPresent;
  }

  public void setPresent(boolean present) {
    isPresent = present;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Proof proof = (Proof) o;
    for (int i = 0; i < path.size(); i++) {
      if (!Arrays.equals(path.get(i), proof.path.get(i))) {
        return false;
      }
    }
    return isPresent == proof.isPresent && root.equals(proof.root);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path, root, isPresent);
  }
}
