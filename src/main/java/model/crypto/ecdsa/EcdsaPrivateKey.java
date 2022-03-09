package model.crypto.ecdsa;


import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import crypto.Sha3256Hasher;
import model.Entity;
import model.codec.EncodedEntity;
import model.crypto.Sha3256Hash;
import modules.codec.JsonEncoder;

/**
 * ECDSA private key implementation.
 */
public class EcdsaPrivateKey extends model.crypto.PrivateKey {

  private static final String SIGN_ALG_SHA_3_256_WITH_ECDSA = "SHA3-256withECDSA";
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
    Signature ecdsaSign;
    KeyFactory keyFactory;
    PrivateKey privateKey;
    byte[] signatureBytes;
    try {
      ecdsaSign = Signature.getInstance(SIGN_ALG_SHA_3_256_WITH_ECDSA);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(SIGN_ALG_SHA_3_256_WITH_ECDSA + "algorithm not found.", ex);
    }
    try {
      keyFactory = KeyFactory.getInstance("EC");
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(SIGN_ALG_SHA_3_256_WITH_ECDSA + "algorithm not found.", ex);
    }
    EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
    try {
      privateKey = keyFactory.generatePrivate(privateKeySpec);
    } catch (InvalidKeySpecException ex) {
      throw new IllegalStateException("key spec is invalid.", ex);
    }
    try {
      ecdsaSign.initSign(privateKey);
    } catch (InvalidKeyException ex) {
      throw new IllegalStateException("key is invalid", ex);
    }
    JsonEncoder encoder = new JsonEncoder();
    EncodedEntity encodedEntity = encoder.encode(e);
    Sha3256Hasher hasher = new Sha3256Hasher();
    Sha3256Hash hash = hasher.computeHash(encodedEntity);
    try {
      ecdsaSign.update(hash.getHashBytes());
      signatureBytes = ecdsaSign.sign();
    } catch (SignatureException ex) {
      throw new IllegalStateException("signature is not initialed correctly.", ex);
    }
    return new EcdsaSignature(signatureBytes, e.id());
  }

  public byte[] getPrivateKeyBytes() {
    return privateKeyBytes;
  }
}
