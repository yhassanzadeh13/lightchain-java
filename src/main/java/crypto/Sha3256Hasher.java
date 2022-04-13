package crypto;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import model.codec.EncodedEntity;
import model.crypto.Sha3256Hash;
import model.lightchain.Identifier;

/**
 * Implements SHA3-256 hashing functionality.
 */
public class Sha3256Hasher implements Hasher {

  private static final String HASH_ALG_SHA_3_256 = "SHA3-256";
  private static final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

  private static byte[] concat(final byte[] e1, final byte[] e2) {
    byte[] res = new byte[e1.length + e2.length];
    System.arraycopy(e1, 0, res, 0, e1.length);
    System.arraycopy(e2, 0, res, e1.length, e2.length);
    return res;
  }

  /**
   * Computes hash of the given encoded entity.
   *
   * @param e input encoded entity.
   * @return SHA3-256 hash object of the entity.
   */
  @Override
  public Sha3256Hash computeHash(EncodedEntity e) {
    try {
      MessageDigest md = MessageDigest.getInstance(HASH_ALG_SHA_3_256);
      byte[] hashValue = md.digest(e.getBytes());
      return new Sha3256Hash(hashValue);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(HASH_ALG_SHA_3_256 + "algorithm not found.", ex);
    }
  }

  public Sha3256Hash computeHash(Identifier id) {
    try {
      MessageDigest md = MessageDigest.getInstance(HASH_ALG_SHA_3_256);
      byte[] hashValue = md.digest(id.getBytes());
      return new Sha3256Hash(hashValue);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(HASH_ALG_SHA_3_256 + "algorithm not found.", ex);
    }
  }

  public Sha3256Hash computeHash(byte[] b1, byte[] b2) {
    try {
      MessageDigest md = MessageDigest.getInstance(HASH_ALG_SHA_3_256);
      byte[] hashValue = md.digest(concat(b1, b2));
      return new Sha3256Hash(hashValue);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(HASH_ALG_SHA_3_256 + "algorithm not found.", ex);
    }
  }
}
