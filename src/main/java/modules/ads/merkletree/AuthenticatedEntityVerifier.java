package modules.ads.merkletree;

import java.util.ArrayList;
import java.util.Arrays;

import crypto.Sha3256Hasher;
import model.crypto.Sha3256Hash;
import modules.ads.AuthenticatedEntity;
import modules.ads.MembershipProof;

/**
 * Verifies the AuthenticatedEntity against its self-contained proof.
 */
public class AuthenticatedEntityVerifier implements modules.ads.AuthenticatedEntityVerifier {
  private static final Sha3256Hasher hasher = new Sha3256Hasher();

  /**
   * Verifies the AuthenticatedEntity against its self-contained proof.
   *
   * @param authenticatedEntity the AuthenticatedEntity to verify.
   * @return true if entity contains a valid Merkle Proof against its root identifier, false otherwise.
   */
  @Override
  public boolean verify(AuthenticatedEntity authenticatedEntity) {
    MembershipProof proof = authenticatedEntity.getMembershipProof();
    ArrayList<Boolean> isLeftNode = proof.getIsLeftNode();
    Sha3256Hash root = proof.getRoot();
    ArrayList<Sha3256Hash> proofPath = proof.getPath();
    Sha3256Hash initialHash = hasher.computeHash(authenticatedEntity.getEntity().id());
    Sha3256Hash currHash;
    if (isLeftNode.get(0)) {
      currHash = hasher.computeHash(initialHash, proofPath.get(0));
    } else {
      currHash = hasher.computeHash(proofPath.get(0), initialHash);
    }
    for (int i = 1; i < proofPath.size(); i++) {
      if (isLeftNode.get(i)) {
        currHash = hasher.computeHash(currHash, proofPath.get(i));
      } else {
        currHash = hasher.computeHash(proofPath.get(i), currHash);
      }
    }
    return Arrays.equals(root.getBytes(), currHash.getBytes());
  }
}