package model.crypto.ecdsa;

import model.crypto.KeyGen;
import model.crypto.PrivateKey;
import model.crypto.PublicKey;

public class EcdsaKeyGen implements KeyGen {

  @Override
  public PrivateKey getPrivateKey() {
    return null;
  }

  @Override
  public PublicKey getPublicKey() {
    return null;
  }

  /**
   * Algorithm behind key generation.
   *
   * @return name of algorithm that is used to generate keys.
   */
  @Override
  public String getAlgorithm() {
    return null;
  }
}
