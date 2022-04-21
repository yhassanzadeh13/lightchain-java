package model.crypto.ecdsa;

import java.io.Serializable;

import model.codec.EntityType;
import model.crypto.Signature;
import model.lightchain.Identifier;

 /**
 * ECDSA signature implementation with signer ID.
 */
public class EcdsaSignature extends Signature implements Serializable {
  public static final String ELLIPTIC_CURVE = "EC";
  public static final String SIGN_ALG_SHA_3_256_WITH_ECDSA = "SHA3-256withECDSA";

  public EcdsaSignature(byte[] bytes, Identifier signerId) {
    super(bytes, signerId);
  }

  @Override
  public String type() {
    return EntityType.TYPE_ECDSA_SIGNATURE;
  }
}
