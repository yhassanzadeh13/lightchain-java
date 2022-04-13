package modules.ads.merkletree;

import model.crypto.Sha3256Hash;
import model.lightchain.Identifier;
import modules.ads.MembershipProof;

import java.util.ArrayList;

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

  public void setRoot(Sha3256Hash root) {
    this.root = root;
  }
}
