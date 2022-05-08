package modules.ads.mtrie;

import model.crypto.Sha3256Hash;
import modules.ads.MembershipProof;
import modules.ads.merkletree.MerklePath;

public class MerklePatriciaTrieProof implements MembershipProof {
  private Sha3256Hash root;
  private MerklePath path;

  /**
   * Root of the authenticated data structure that this proof belongs to.
   *
   * @return hash value of the root node.
   */
  @Override
  public Sha3256Hash getRoot() {
    return null;
  }

  /**
   * Returns the merkle path of the proof of membership.
   *
   * @return merkle path of the proof of membership.
   */
  @Override
  public MerklePath getMerklePath() {
    return null;
  }
}
