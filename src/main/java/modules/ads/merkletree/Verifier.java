package modules.ads.merkletree;

import java.util.ArrayList;
import java.util.Arrays;

import crypto.Sha3256Hasher;
import model.crypto.Sha3256Hash;
import modules.ads.AuthenticatedEntity;
import modules.ads.AuthenticatedEntityVerifier;
import modules.ads.MembershipProof;

public class Verifier implements AuthenticatedEntityVerifier {
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
    Sha3256Hash root = proof.getRoot();
    ArrayList<Sha3256Hash> proofPath = proof.getPath();
    Sha3256Hash currHash = hasher.computeHash(proofPath.get(0), proofPath.get(1));
    for (int i = 2; i < proofPath.size(); i++) {
      currHash = hasher.computeHash(currHash, proofPath.get(i));
    }
    return Arrays.equals(root.getBytes(), currHash.getBytes());
  }
}