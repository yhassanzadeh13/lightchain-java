package model.crypto.ecdsa;

import java.security.*;
import java.security.spec.ECGenParameterSpec;

import model.crypto.KeyGen;

/**
 * This class generates an ECDSA key pair.
 */
public class EcdsaKeyGen implements KeyGen {

  private static final String ellipticCurve = "secp256r1";
  private static final String SIGN_ALG_SHA_3_256_WITH_ECDSA = "SHA3-256withECDSA";
  private final EcdsaPrivateKey privateKey;
  private final EcdsaPublicKey publicKey;

  /**
   * Constructor for ECDSA key generation.
   */
  public EcdsaKeyGen() {
    ECGenParameterSpec ecSpec = new ECGenParameterSpec(ellipticCurve);
    KeyPairGenerator g = null;
    try {
      g = KeyPairGenerator.getInstance("EC");
      g.initialize(ecSpec, new SecureRandom());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(SIGN_ALG_SHA_3_256_WITH_ECDSA + " algorithm not found", e);
    } catch (InvalidAlgorithmParameterException e) {
      throw new IllegalStateException(ellipticCurve + " algorithm parameter not found", e);
    }
    KeyPair keypair = g.generateKeyPair();
    PublicKey publicKey = keypair.getPublic();
    PrivateKey privateKey = keypair.getPrivate();
    this.publicKey = new EcdsaPublicKey(publicKey.getEncoded());
    this.privateKey = new EcdsaPrivateKey(privateKey.getEncoded());
  }

  @Override
  public model.crypto.ecdsa.EcdsaPrivateKey getPrivateKey() {
    return this.privateKey;
  }

  @Override
  public model.crypto.ecdsa.EcdsaPublicKey getPublicKey() {
    return this.publicKey;
  }

  /**
   * Algorithm behind key generation.
   *
   * @return name of algorithm that is used to generate keys.
   */
  @Override
  public String getAlgorithm() {
    return SIGN_ALG_SHA_3_256_WITH_ECDSA;
  }
}
