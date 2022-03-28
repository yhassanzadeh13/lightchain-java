package modules.ads.skiplist;

import java.util.ArrayList;
import java.util.Arrays;

import crypto.Sha3256Hasher;
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
    byte[] root = proof.getRoot().getHashBytes();
    ArrayList<byte[]> proofPath = proof.getPath();
    byte[] currHash = hasher.computeHash(proofPath.get(0), proofPath.get(1)).getHashBytes();
    for (int i = 2; i < proofPath.size(); i++) {
      currHash = hasher.computeHash(proofPath.get(i), currHash).getHashBytes();
    }
    System.out.println(Arrays.toString(currHash));
    System.out.println(Arrays.toString(root));
    return Arrays.equals(root, currHash);
  }
}
