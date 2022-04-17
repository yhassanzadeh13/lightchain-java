package modules.ads.merkletree;

import model.Entity;
import modules.ads.MembershipProof;

/**
 * An entity with its membership proof and type.
 */
public class AuthenticatedLightChainEntity extends modules.ads.AuthenticatedEntity {
  private MembershipProof membershipProof;
  private String type;
  private Entity entity;

  /**
   * Constructor of an authenticated entity.
   *
   * @param proof the membership proof
   * @param type  the type of the entity
   * @param e     the entity
   */
  public AuthenticatedLightChainEntity(Proof proof, String type, Entity e) {
    this.membershipProof = new Proof(proof.getPath(), proof.getRoot());
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
    return new Proof(membershipProof.getPath(), membershipProof.getRoot());
  }

  public void setMembershipProof(MembershipProof membershipProof) {
    this.membershipProof = new Proof(membershipProof.getPath(), membershipProof.getRoot());;
  }
}
