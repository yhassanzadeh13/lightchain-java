package modules.ads.merkletree;

import model.crypto.Sha3256Hash;
import model.lightchain.Identifier;
import modules.ads.MembershipProof;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class Proof implements MembershipProof {
  private ArrayList<Sha3256Hash> path;
  private Sha3256Hash root;

  public Proof(ArrayList<Sha3256Hash> path, Sha3256Hash root) {
    this.path = path;
    this.root = root;
  }

  @Override
  public ArrayList<Sha3256Hash> getPath() {
    return path;
  }

  public void setPath(ArrayList<Sha3256Hash> path) {
    this.path = path;
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
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
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
