package unittest.fixtures;

import model.crypto.Signature;
import model.lightchain.Identifier;
import model.lightchain.ValidatedTransaction;
import protocol.Parameters;

/**
 * Encapsulates creating validated transactions with random content for fixture.
 */
public class ValidatedTransactionFixture {
  /**
   * Constructor of the validated transactions with randomly generated parameters.
   *
   * @return random ValidatedTransaction object.
   */
  public static ValidatedTransaction newValidatedTransaction() {
    Identifier referenceBlockId = IdentifierFixture.newIdentifier();
    Identifier sender = IdentifierFixture.newIdentifier();
    Identifier receiver = IdentifierFixture.newIdentifier();
    double amount = 100;
    int certificatesSize = Parameters.SIGNATURE_THRESHOLD;
    Signature[] certificates = new Signature[certificatesSize];
    for (int i = 0; i < certificatesSize; i++) {
      certificates[i] = SignatureFixture.newSignatureFixture();
    }
    ValidatedTransaction valTrans = new ValidatedTransaction(referenceBlockId, sender, receiver, amount, certificates);
    Signature sign = SignatureFixture.newSignatureFixture();
    valTrans.setSignature(sign);
    return valTrans;
  }

  /**
   * Constructor of the validated transactions with randomly generated parameters and given sender identifier.
   *
   * @param sender identifier of the sender of this transaction.
   * @return random ValidatedTransaction object.
   */
  public static ValidatedTransaction newValidatedTransaction(Identifier sender) {
    Identifier referenceBlockId = IdentifierFixture.newIdentifier();
    Identifier receiver = IdentifierFixture.newIdentifier();
    double amount = 100;
    int certificatesSize = Parameters.SIGNATURE_THRESHOLD;
    Signature[] certificates = new Signature[certificatesSize];
    for (int i = 0; i < certificatesSize; i++) {
      certificates[i] = SignatureFixture.newSignatureFixture();
    }
    ValidatedTransaction valTrans = new ValidatedTransaction(referenceBlockId, sender, receiver, amount, certificates);
    Signature sign = SignatureFixture.newSignatureFixture();
    valTrans.setSignature(sign);
    return valTrans;
  }


  /**
   * Constructor of the validated transactions with randomly generated parameters and given sender and receiver identifier.
   *
   * @param sender   identifier of the sender of this transaction.
   * @param receiver identifier of the receiver of this transaction.
   * @return random ValidatedTransaction object.
   */
  public static ValidatedTransaction newValidatedTransaction(Identifier sender, Identifier receiver) {
    Identifier referenceBlockId = IdentifierFixture.newIdentifier();
    double amount = 100;
    int certificatesSize = Parameters.SIGNATURE_THRESHOLD;
    Signature[] certificates = new Signature[certificatesSize];
    for (int i = 0; i < certificatesSize; i++) {
      certificates[i] = SignatureFixture.newSignatureFixture();
    }
    ValidatedTransaction valTrans = new ValidatedTransaction(referenceBlockId, sender, receiver, amount, certificates);
    Signature sign = SignatureFixture.newSignatureFixture();
    valTrans.setSignature(sign);
    return valTrans;
  }

  /**
   * Constructor of the validated transactions with randomly generated parameters and given sender identifier.
   *
   * @param certificatesSize size of the certificates array.
   * @return random ValidatedTransaction object.
   */
  public static ValidatedTransaction newValidatedTransaction(int certificatesSize) {
    Identifier referenceBlockId = IdentifierFixture.newIdentifier();
    Identifier sender = IdentifierFixture.newIdentifier();
    Identifier receiver = IdentifierFixture.newIdentifier();
    double amount = 100;
    Signature[] certificates = new Signature[certificatesSize];
    for (int i = 0; i < certificatesSize; i++) {
      certificates[i] = SignatureFixture.newSignatureFixture();
    }
    ValidatedTransaction valTrans = new ValidatedTransaction(referenceBlockId, sender, receiver, amount, certificates);
    Signature sign = SignatureFixture.newSignatureFixture();
    valTrans.setSignature(sign);
    return valTrans;
  }

  /**
   * Constructor of the validated transactions with randomly generated parameters and given reference block id,
   * sender and receiver identifier.
   *
   * @param referenceBlockId identifier of the reference block.
   * @param sender           identifier of the sender of this transaction.
   * @param receiver         identifier of the receiver of this transaction.
   * @return random ValidatedTransaction object.
   */
  public static ValidatedTransaction newValidatedTransaction(Identifier referenceBlockId, Identifier sender, Identifier receiver) {
    double amount = 100;
    int certificatesSize = Parameters.SIGNATURE_THRESHOLD;
    Signature[] certificates = new Signature[certificatesSize];
    for (int i = 0; i < certificatesSize; i++) {
      certificates[i] = SignatureFixture.newSignatureFixture();
    }
    ValidatedTransaction valTrans = new ValidatedTransaction(referenceBlockId, sender, receiver, amount, certificates);
    Signature sign = SignatureFixture.newSignatureFixture();
    valTrans.setSignature(sign);
    return valTrans;
  }
}
