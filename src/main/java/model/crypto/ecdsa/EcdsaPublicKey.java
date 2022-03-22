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

  private static final String SIGN_ALG_SHA_3_256_WITH_ECDSA = "SHA3-256withECDSA";

  public EcdsaPublicKey(byte[] bytes) {
    super(bytes);
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
    Signature ecdsaVerify;
    KeyFactory keyFactory;
    PublicKey publicKey;
    try {
      ecdsaVerify = Signature.getInstance(SIGN_ALG_SHA_3_256_WITH_ECDSA);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(SIGN_ALG_SHA_3_256_WITH_ECDSA + " algorithm not found", ex);
    }
    try {
      keyFactory = KeyFactory.getInstance("EC");
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(SIGN_ALG_SHA_3_256_WITH_ECDSA + " algorithm not found", ex);
    }
    EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(this.bytes);
    try {
      publicKey = keyFactory.generatePublic(publicKeySpec);
    } catch (InvalidKeySpecException ex) {
      throw new IllegalStateException("key spec is invalid", ex);
    }
    try {
      ecdsaVerify.initVerify(publicKey);
    } catch (InvalidKeyException ex) {
      throw new IllegalStateException("key is invalid", ex);
    }
    JsonEncoder encoder = new JsonEncoder();
    EncodedEntity encodedEntity = encoder.encode(e);
    Sha3256Hasher hasher = new Sha3256Hasher();
    Sha3256Hash hash = hasher.computeHash(encodedEntity);
    try {
      ecdsaVerify.update(hash.getBytes());
      return ecdsaVerify.verify(s.getBytes());
    } catch (SignatureException ex) {
      throw new IllegalStateException("signature is not initialed correctly", ex);
    }
  }

  public byte[] getPublicKeyBytes() {
    return this.bytes.clone();
  }
}
