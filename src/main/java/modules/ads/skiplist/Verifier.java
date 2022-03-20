package modules.ads.skiplist;

import modules.ads.AuthenticatedEntity;
import modules.ads.AuthenticatedEntityVerifier;

public class Verifier implements AuthenticatedEntityVerifier {
  /**
   * Verifies the AuthenticatedEntity against its self-contained proof.
   *
   * @param authenticatedEntity the AuthenticatedEntity to verify.
   * @return true if entity contains a valid Merkle Proof against its root identifier, false otherwise.
   */
  @Override
  public boolean verify(AuthenticatedEntity authenticatedEntity) {
    return false;
  }
}
