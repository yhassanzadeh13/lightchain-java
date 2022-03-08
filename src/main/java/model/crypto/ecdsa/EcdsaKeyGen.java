package model.crypto.ecdsa;

import model.crypto.KeyGen;
import model.crypto.PrivateKey;
import model.crypto.PublicKey;

public class EcdsaKeyGen extends KeyGen {
  public EcdsaKeyGen(PrivateKey privateKey, PublicKey publicKey) {
    super(privateKey, publicKey);
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
