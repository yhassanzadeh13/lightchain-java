package modules.ads.skiplist;

import model.Entity;
import modules.ads.MembershipProof;

public class AuthenticatedEntity extends modules.ads.AuthenticatedEntity {

  private MembershipProof membershipProof;
  private String type;
  private Entity entity;

  public AuthenticatedEntity(MembershipProof membershipProof, String type, Entity entity) {
    this.membershipProof = membershipProof;
    this.type = type;
    this.entity = entity;
  }

  @Override
  public MembershipProof getMembershipProof() {
    return membershipProof;
  }

  public void setMembershipProof(MembershipProof membershipProof) {
    this.membershipProof = membershipProof;
  }

  /**
   * Type of this entity.
   *
   * @return type of this entity.
   */
  @Override
  public String type() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Override
  public Entity getEntity() {
    return entity;
  }

  public void setEntity(Entity entity) {
    this.entity = entity;
  }
}
