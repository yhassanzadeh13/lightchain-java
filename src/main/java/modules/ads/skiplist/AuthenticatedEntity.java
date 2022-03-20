package modules.ads.skiplist;

import model.Entity;
import modules.ads.MembershipProof;

public class AuthenticatedEntity extends modules.ads.AuthenticatedEntity{

  /**
   * Type of this entity.
   *
   * @return type of this entity.
   */
  @Override
  public String type() {
    return null;
  }

  @Override
  public Entity getEntity() {
    return null;
  }

  @Override
  public MembershipProof getMembershipProof() {
    return null;
  }
}
