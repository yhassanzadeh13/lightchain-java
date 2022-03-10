package modules.ads;

/**
 * Verifies an AuthenticatedEntity against its self-contained proof.
 */
public interface AuthenticatedEntityVerifier {
  /**
   * Verifies the AuthenticatedEntity against its self-contained proof.
   *
   * @param authenticatedEntity the AuthenticatedEntity to verify.
   * @return true if entity contains a valid Merkle Proof against its root identifier, false otherwise.
   */
  boolean verify(AuthenticatedEntity authenticatedEntity);
}
