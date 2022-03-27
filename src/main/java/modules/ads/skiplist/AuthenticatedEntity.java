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

  public void setType(String type) {
    this.type = type;
  }

  @Override
  public String type() {
    return null;
  }

  @Override
  public Entity getEntity() {
    return null;
  }

  public void setEntity(Entity entity) {
    this.entity = entity;
  }

  @Override
  public MembershipProof getMembershipProof() {
    return null;
  }

  public void setMembershipProof(MembershipProof membershipProof) {
    this.membershipProof = membershipProof;
  }
}
