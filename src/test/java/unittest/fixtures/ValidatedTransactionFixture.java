package unittest.fixtures;

import java.util.ArrayList;
import java.util.Random;

import model.crypto.Signature;
import model.lightchain.Account;
import model.lightchain.Identifier;
import model.lightchain.ValidatedTransaction;
import protocol.Parameters;

/**
 * Encapsulates creating validated transactions with random content for fixture.
 */
public class ValidatedTransactionFixture {
  private static final Random random = new Random();

  /**
   * Constructor of the validated transactions with randomly generated parameters.
   *
   * @return random ValidatedTransaction object.
   */
  public static ValidatedTransaction newValidatedTransaction() {
    return ValidatedTransactionFixture.newValidatedTransaction(
        IdentifierFixture.newIdentifier(),
        IdentifierFixture.newIdentifier(),
        Parameters.SIGNATURE_THRESHOLD);
  }

  /**
   * Constructor of the validated transactions with randomly generated parameters and given sender identifier.
   *
   * @param sender identifier of the sender of this transaction.
   * @return random ValidatedTransaction object.
   */
  public static ValidatedTransaction newValidatedTransaction(Identifier sender) {
    return newValidatedTransaction(sender, IdentifierFixture.newIdentifier(), Parameters.SIGNATURE_THRESHOLD);
  }

  /**
   * Constructor of the validated transactions with randomly generated parameters and given size of the
   * certificates array.
   *
   * @param certificatesSize size of the certificates array.
   * @return random ValidatedTransaction object.
   */
  public static ValidatedTransaction newValidatedTransaction(
      Identifier sender,
      Identifier receiver,
      int certificatesSize) {

    Identifier referenceBlockId = IdentifierFixture.newIdentifier();

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
   * Creates array of validated transactions.
   *
   * @param accounts accounts to pick sender and receiver of transactions.
   * @param count    total transactions to be created.
   * @return array of validated transactions.
   */
  public static ValidatedTransaction[] newValidatedTransactions(ArrayList<Account> accounts, int count) {
    ValidatedTransaction[] transactions = new ValidatedTransaction[count];

    for (int i = 0; i < count; i++) {
      int senderIndex = random.nextInt(accounts.size());
      int receiverIndex = random.nextInt(accounts.size());
      while (receiverIndex == senderIndex) {
        receiverIndex = random.nextInt(accounts.size());
      }
      transactions[i] = ValidatedTransactionFixture.newValidatedTransaction(
          accounts.get(senderIndex).getIdentifier(),
          accounts.get(receiverIndex).getIdentifier(),
          Parameters.SIGNATURE_THRESHOLD);
    }

    return transactions;
  }

  /**
   * Creates array of validated transactions.
   *
   * @param count    total transactions to be created.
   * @return array of validated transactions.
   */
  public static ValidatedTransaction[] newValidatedTransactions(int count) {
    ValidatedTransaction[] transactions = new ValidatedTransaction[count];
    for(int i = 0; i < count; i++) {
      transactions[i] = newValidatedTransaction();
    }

    return transactions;
  }
}