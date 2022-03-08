package crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import model.codec.EncodedEntity;
import model.crypto.Hash;
import model.crypto.Sha3256Hash;

/**
 * Implements SHA3-256 hashing functionality.
 */
public class Sha3256Hasher implements Hasher {

  private static final String HASH_ALG_SHA_3_256 = "SHA3-256";

  /**
   * Computes hash of the given encoded entity.
   *
   * @param e input encoded entity.
   * @return SHA3-256 hash object of the entity.
   */
  @Override
  public Sha3256Hash computeHash(EncodedEntity e) {
    // TODO: implement it
    try {
      MessageDigest md = MessageDigest.getInstance(HASH_ALG_SHA_3_256);
      byte[] hashValue = md.digest(e.getBytes());
      return new Sha3256Hash(hashValue);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(HASH_ALG_SHA_3_256 + "algorithm not found.", ex);
    }
  }
}
