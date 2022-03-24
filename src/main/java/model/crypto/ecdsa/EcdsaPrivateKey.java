package model.crypto.ecdsa;


import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import model.Entity;

/**
 * ECDSA private key implementation.
 */
public class EcdsaPrivateKey extends model.crypto.PrivateKey {

  private final PrivateKey ecdsaPrivateKey;

  /**
   * Constructs an Ecdsa private key from the given encoded private key.
   *
   * @param bytes encoded private key bytes.
   */
  public EcdsaPrivateKey(byte[] bytes) throws IllegalStateException{
    super(bytes);
    KeyFactory keyFactory;
    try {
      keyFactory = KeyFactory.getInstance(EcdsaSignature.ELLIPTIC_CURVE);
      EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(bytes);
      ecdsaPrivateKey = keyFactory.generatePrivate(privateKeySpec);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(EcdsaSignature.ELLIPTIC_CURVE + "algorithm not found", e);
    } catch (InvalidKeySpecException e) {
      throw new IllegalStateException("key spec is invalid", e);
    }
  }

  /**
   * Signs the given entity using private key.
   *
   * @param e entity to sign.
   * @return a signature over entity e using private key.
   */
  @Override
  public model.crypto.Signature signEntity(Entity e) throws IllegalStateException{
    byte[] signatureBytes;
    try {
      Signature ecdsaSign = Signature.getInstance(EcdsaSignature.SIGN_ALG_SHA_3_256_WITH_ECDSA);
      ecdsaSign.initSign(ecdsaPrivateKey);
      ecdsaSign.update(e.id().getBytes());
      signatureBytes = ecdsaSign.sign();
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(EcdsaSignature.SIGN_ALG_SHA_3_256_WITH_ECDSA + "algorithm not found", ex);
    } catch (InvalidKeyException ex) {
      throw new IllegalStateException("key is invalid", ex);
    } catch (SignatureException ex) {
      throw new IllegalStateException("signature is not initialed correctly", ex);
    }
    return new EcdsaSignature(signatureBytes, e.id());
  }

}
