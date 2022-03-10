package modules.ads;

import model.lightchain.Identifier;

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
   * Sibling of the given identifier on the membership Merkle Proof path to the root.
   *
   * @param identifier identifier of the entity.
   * @return sibling of given identifier.
   */
  Identifier getSiblingOf(Identifier identifier);
}
