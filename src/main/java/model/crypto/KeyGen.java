package model.crypto;

/**
 * KeyGen represents the cryptographic algorithm to generate a pair of public and private keys.
 */
public abstract class KeyGen {
  /**
   * Represents the private key in this pair.
   */
  private final PrivateKey privateKey;

  /**
   * Represents the public key in this pair.
   */
  private final PublicKey publicKey;

  public KeyGen(PrivateKey privateKey, PublicKey publicKey) {
    this.privateKey = privateKey;
    this.publicKey = publicKey;
  }

  public PrivateKey getPrivateKey() {
    return privateKey;
  }

  public PublicKey getPublicKey() {
    return publicKey;
  }

  /**
   * Algorithm behind key generation.
   *
   * @return name of algorithm that is used to generate keys.
   */
  abstract String getAlgorithm();
}
