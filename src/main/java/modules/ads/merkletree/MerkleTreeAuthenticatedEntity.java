package modules.ads.merkletree;

import java.io.Serializable;

import model.Entity;
import modules.ads.MembershipProof;

/**
 * An entity with its membership proof and type.
 */
public class MerkleTreeAuthenticatedEntity extends modules.ads.AuthenticatedEntity implements Serializable {
  private final MembershipProof membershipProof;
  private final String type;
  private final Entity entity;

  /**
   * Constructor of an authenticated entity.
   *
   * @param proof the membership proof
   * @param type  the type of the entity
   * @param e     the entity
   */
  public MerkleTreeAuthenticatedEntity(MerkleProof proof, String type, Entity e) {
    this.membershipProof = new MerkleProof(proof.getPath(), proof.getRoot(), proof.getIsLeftNode());
    this.type = type;
    this.entity = e;
  }

  @Override
  public String type() {
    return type;
  }

  @Override
  public Entity getEntity() {
    return entity;
  }

  @Override
  public MembershipProof getMembershipProof() {
    return new MerkleProof(membershipProof.getPath(), membershipProof.getRoot(), membershipProof.getIsLeftNode());
  }
}
