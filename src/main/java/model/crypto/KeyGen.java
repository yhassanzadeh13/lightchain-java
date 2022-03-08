package model.crypto;

/**
 * KeyGen represents the cryptographic algorithm to generate a pair of public and private keys.
 */
public interface KeyGen {

  PrivateKey getPrivateKey();

  PublicKey getPublicKey();

  /**
   * Algorithm behind key generation.
   *
   * @return name of algorithm that is used to generate keys.
   */
  String getAlgorithm();
}
