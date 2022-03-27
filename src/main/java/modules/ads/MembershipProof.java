package modules.ads;

import java.util.ArrayList;
import java.util.Stack;

import model.crypto.Sha3256Hash;
import model.lightchain.Identifier;
import modules.ads.skiplist.SkipListNode;

/**
 * Represents a Merkle Proof of membership against a certain root identifier.
 */
public interface MembershipProof {
  /**
   * Root of the authenticated data structure that this proof belongs to.
   *
   * @return root identifier.
   */
  Identifier getRoot();

  /**
   * Returns the path of the proof of membership.
   *
   * @param identifier identifier to be verified.
   * @return path of the proof of membership.
   */
  ArrayList<Sha3256Hash> getPath(Identifier identifier);
}
