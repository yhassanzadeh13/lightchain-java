package unittest.fixtures;

import model.crypto.Signature;
import model.lightchain.Identifier;

public class SignatureFixture extends Signature {
  public SignatureFixture(byte[] bytes, Identifier signerId) {
    super(bytes, signerId);
  }
}
