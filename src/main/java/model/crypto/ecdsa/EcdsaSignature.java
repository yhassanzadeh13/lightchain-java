package model.crypto.ecdsa;

import model.crypto.Signature;
import model.lightchain.Identifier;

public class EcdsaSignature extends Signature {
  public EcdsaSignature(byte[] bytes, Identifier signerId) {
    super(bytes, signerId);
  }
}
