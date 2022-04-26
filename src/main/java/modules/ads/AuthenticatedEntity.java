package modules.ads;

import java.io.Serializable;

import model.Entity;

/**
 * AuthenticatedEntity is a wrapper model around the Entity type that also contains a membership Merkle Proof for
 * that entity against a root identifier.
 */
public abstract class AuthenticatedEntity extends Entity implements Serializable {
  public abstract Entity getEntity();

  public abstract MembershipProof getMembershipProof();
}
