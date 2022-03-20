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

  public Sha3256Hash computeHash(Identifier i, Sha3256Hash h) {
    try {
      MessageDigest md = MessageDigest.getInstance(HASH_ALG_SHA_3_256);
      int compare = Arrays.compare(i.getBytes(), h.getHashBytes());
      if (compare > 0) {
        return new Sha3256Hash(md.digest(concat(i.getBytes(), h.getHashBytes())));
      }
      return new Sha3256Hash(md.digest(concat(h.getHashBytes(), i.getBytes())));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(HASH_ALG_SHA_3_256 + "algorithm not found.", ex);
    }
  }

  public Sha3256Hash computeHash(Identifier i1, Identifier i2) {
    try {
      MessageDigest md = MessageDigest.getInstance(HASH_ALG_SHA_3_256);
      int compare = Arrays.compare(i1.getBytes(), i2.getBytes());
      if (compare > 0) {
        return new Sha3256Hash(md.digest(concat(i1.getBytes(), i2.getBytes())));
      }
      return new Sha3256Hash(md.digest(concat(i2.getBytes(), i1.getBytes())));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(HASH_ALG_SHA_3_256 + "algorithm not found.", ex);
    }
  }


  public Sha3256Hash computeHash(Sha3256Hash h1, Sha3256Hash h2) {
    try {
      MessageDigest md = MessageDigest.getInstance(HASH_ALG_SHA_3_256);
      int compare = Arrays.compare(h1.getHashBytes(), h2.getHashBytes());
      if (compare > 0) {
        return new Sha3256Hash(md.digest(concat(h1.getHashBytes(), h2.getHashBytes())));
      }
      return new Sha3256Hash(md.digest(concat(h2.getHashBytes(), h1.getHashBytes())));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(HASH_ALG_SHA_3_256 + "algorithm not found.", ex);
    }
  }
}
