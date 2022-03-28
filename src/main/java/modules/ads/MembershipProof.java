package modules.ads;

import java.util.ArrayList;

import model.crypto.Sha3256Hash;

/**
 * Represents a Merkle Proof of membership against a certain root identifier.
 */
public interface MembershipProof {
  /**
   * Root of the authenticated data structure that this proof belongs to.
   *
   * @return root identifier.
   */
  Sha3256Hash getRoot();

  /**
   * Returns the path of the proof of membership.
   *
   * @return path of the proof of membership.
   */
  ArrayList<byte[]> getPath();
}
