package modules.ads.skiplist;

import model.lightchain.Identifier;
import modules.ads.MembershipProof;

public class Proof implements MembershipProof {
  /**
   * Root of the authenticated data structure that this proof belongs to.
   *
   * @return root identifier.
   */
  @Override
  public Identifier getRoot() {
    return null;
  }

  /**
   * Sibling of the given identifier on the membership Merkle Proof path to the root.
   *
   * @param identifier identifier of the entity.
   * @return sibling of given identifier.
   */
  @Override
  public Identifier getSiblingOf(Identifier identifier) {
    return null;
  }
}
