package modules.ads.merkletree;

import model.Entity;
import modules.ads.MembershipProof;

public class AuthenticatedEntity extends modules.ads.AuthenticatedEntity {
  private MembershipProof membershipProof;
  private String type;
  private Entity entity;

  public AuthenticatedEntity(Proof proof, String type, Entity e) {
    this.membershipProof = proof;
    this.type = type;
    this.entity = e;
  }

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
  @Override
  public MembershipProof getMembershipProof() {
    return membershipProof;
  }

  public void setMembershipProof(MembershipProof membershipProof) {
    this.membershipProof = membershipProof;
  }
}
