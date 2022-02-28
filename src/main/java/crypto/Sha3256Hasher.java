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
  /**
   * Computes hash of the given encoded entity.
   *
   * @param e input encoded entity.
   * @return SHA3-256 hash object of the entity.
   */
  @Override
  public Hash computeHash(EncodedEntity e) {
    // TODO: implement it
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("SHA3-256");
      byte[] hashValue = md.digest(e.getBytes());
      return new Sha3256Hash(hashValue);
    } catch (NoSuchAlgorithmException ex) {
      ex.printStackTrace();
    }
    return null;
  }
}
