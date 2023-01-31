package model.crypto.ecdsa;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import model.Entity;

/**
 * Represents an ECDSA public key.
 */
public class EcdsaPublicKey extends model.crypto.PublicKey {

  private final PublicKey ecdsaPublicKey;

  /**
   * Constructs an Ecdsa public key from the given encoded public key.
   *
   * @param bytes encoded public key bytes.
   */
  public EcdsaPublicKey(byte[] bytes) throws IllegalStateException {
    super(bytes);
    KeyFactory keyFactory;
    try {
      keyFactory = KeyFactory.getInstance(EcdsaSignature.ELLIPTIC_CURVE);
      EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(this.bytes);
      ecdsaPublicKey = keyFactory.generatePublic(publicKeySpec);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(EcdsaSignature.ELLIPTIC_CURVE + "algorithm not found", e);
    } catch (InvalidKeySpecException e) {
      throw new IllegalStateException("key spec is invalid", e);
    }
  }

  /**
   * Implements signature verification.
   *
   * @param e entity that carries a signature on.
   * @param s digital signature over the entity.
   * @return true if s carries a valid signature over e against this public key, false otherwise.
   */
  @Override
  public boolean verifySignature(Entity e, model.crypto.Signature s) throws IllegalStateException {
    try {
      Signature ecdsaVerify = Signature.getInstance(EcdsaSignature.SIGN_ALG_SHA_3_256_WITH_ECDSA);
      ecdsaVerify.initVerify(ecdsaPublicKey);
      ecdsaVerify.update(e.id().getBytes());
      return ecdsaVerify.verify(s.getBytes());
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(EcdsaSignature.SIGN_ALG_SHA_3_256_WITH_ECDSA + " algorithm not found", ex);
    } catch (InvalidKeyException ex) {
      throw new IllegalStateException("key is invalid", ex);
    } catch (SignatureException ex) {
      throw new IllegalStateException("signature is not initialed correctly", ex);
    }
  }

}
