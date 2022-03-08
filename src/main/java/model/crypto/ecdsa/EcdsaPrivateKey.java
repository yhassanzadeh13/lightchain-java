package model.crypto.ecdsa;

import model.Entity;

import java.security.NoSuchAlgorithmException;
import java.security.Signature;

public class EcdsaPrivateKey extends model.crypto.PrivateKey {

  private static final String SIGN_ALG_SHA_3_256_ECDSA = "SHA3-256withECDSA";
  private final byte[] privateKeyBytes;

  public EcdsaPrivateKey(byte[] bytes) {
    super(bytes);
    this.privateKeyBytes = bytes.clone();
  }

  /**
   * Signs the given entity using private key.
   *
   * @param e entity to sign.
   * @return a signature over entity e using private key.
   */
  @Override
  public model.crypto.Signature signEntity(Entity e) {
    try {
      Signature ecdsaSign = Signature.getInstance(SIGN_ALG_SHA_3_256_ECDSA);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(SIGN_ALG_SHA_3_256_ECDSA + "algorithm not found.", ex);
    }
    return null;
  }
}
