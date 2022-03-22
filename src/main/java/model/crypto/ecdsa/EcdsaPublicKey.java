package model.crypto.ecdsa;

import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import crypto.Sha3256Hasher;
import model.Entity;
import model.codec.EncodedEntity;
import model.crypto.Sha3256Hash;
import modules.codec.JsonEncoder;

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
  public EcdsaPublicKey(byte[] bytes) {
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
  public boolean verifySignature(Entity e, model.crypto.Signature s) {
    try {
      Signature ecdsaVerify = Signature.getInstance(EcdsaSignature.SIGN_ALG_SHA_3_256_WITH_ECDSA);
      ecdsaVerify.initVerify(ecdsaPublicKey);
      JsonEncoder encoder = new JsonEncoder();
      EncodedEntity encodedEntity = encoder.encode(e);
      Sha3256Hasher hasher = new Sha3256Hasher();
      Sha3256Hash hash = hasher.computeHash(encodedEntity);
      ecdsaVerify.update(hash.getBytes());
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
