package unittest.fixtures;

import java.util.ArrayList;
import java.util.Random;

import model.crypto.Signature;
import model.lightchain.Account;
import model.lightchain.Identifier;
import model.lightchain.ValidatedBlock;
import model.lightchain.ValidatedTransaction;
import protocol.Parameters;

/**
 * A fixture for testing the ValidatedBlock class.
 */
public class ValidatedBlockFixture {
  private static final Random random = new Random();

  /**
   * Returns a validated block with randomly generated values.
   *
   * @return a validated block with randomly generated values.
   */
  public static ValidatedBlock newValidatedBlock() {
    Identifier previousBlockId = IdentifierFixture.newIdentifier();
    Identifier proposer = IdentifierFixture.newIdentifier();
    int validatedTransactionsSize = Parameters.MIN_TRANSACTIONS_NUM + 2;
    ValidatedTransaction[] transactions = new ValidatedTransaction[validatedTransactionsSize];
    for (int i = 0; i < validatedTransactionsSize; i++) {
      transactions[i] = ValidatedTransactionFixture.newValidatedTransaction();
    }
    Signature signature = SignatureFixture.newSignatureFixture(proposer);
    int certificatesSize = Parameters.SIGNATURE_THRESHOLD;
    Signature[] certificates = new Signature[certificatesSize];
    for (int i = 0; i < certificatesSize; i++) {
      certificates[i] = SignatureFixture.newSignatureFixture();
    }
    int height = Math.abs(random.nextInt(1_000_000));
    return new ValidatedBlock(previousBlockId, proposer, transactions, signature, certificates, height);
  }

  /**
   * Returns a validated block with randomly generated values and given accounts of the block.
   *
   * @return a validated block with randomly generated values and given accounts of the block.
   */
  public static ValidatedBlock newValidatedBlock(ArrayList<Account> accounts) {
    Identifier previousBlockId = IdentifierFixture.newIdentifier();
    Identifier proposer = IdentifierFixture.newIdentifier();
    int validatedTransactionsSize = Parameters.MIN_TRANSACTIONS_NUM + 2;
    ValidatedTransaction[] transactions = new ValidatedTransaction[validatedTransactionsSize];
    for (int i = 0; i < validatedTransactionsSize; i++) {
      int senderIndex = random.nextInt(accounts.size());
      int receiverIndex = random.nextInt(accounts.size());
      while (receiverIndex == senderIndex) {
        receiverIndex = random.nextInt(accounts.size());
      }
      transactions[i] = ValidatedTransactionFixture.newValidatedTransaction(
              accounts.get(senderIndex).getIdentifier(),
              accounts.get(receiverIndex).getIdentifier());
    }
    Signature signature = SignatureFixture.newSignatureFixture(proposer);
    int certificatesSize = Parameters.SIGNATURE_THRESHOLD;
    Signature[] certificates = new Signature[certificatesSize];
    for (int i = 0; i < certificatesSize; i++) {
      certificates[i] = SignatureFixture.newSignatureFixture();
    }
    int height = Math.abs(random.nextInt(1_000_000));
    return new ValidatedBlock(previousBlockId, proposer, transactions, signature, certificates, height);
  }

  /**
   * Returns a validated block with randomly generated values,
   * given height, previous block id and accounts of the block.
   *
   * @param accounts        the accounts of the block.
   * @param height          the height of the block.
   * @param previousBlockId the previous block id of the block.
   * @return a validated block with randomly generated values,
   * given height, previous block id and accounts of the block.
   */
  public static ValidatedBlock newValidatedBlock(ArrayList<Account> accounts, int height, Identifier previousBlockId) {
    Identifier proposer = IdentifierFixture.newIdentifier();
    int validatedTransactionsSize = Parameters.MIN_TRANSACTIONS_NUM + 2;
    ValidatedTransaction[] transactions = new ValidatedTransaction[validatedTransactionsSize];
    for (int i = 0; i < validatedTransactionsSize; i++) {
      int senderIndex = random.nextInt(accounts.size());
      int receiverIndex = random.nextInt(accounts.size());
      while (receiverIndex == senderIndex) {
        receiverIndex = random.nextInt(accounts.size());
      }
      transactions[i] = ValidatedTransactionFixture.newValidatedTransaction(
              accounts.get(senderIndex).getIdentifier(),
              accounts.get(receiverIndex).getIdentifier());
    }
    Signature signature = SignatureFixture.newSignatureFixture(proposer);
    int certificatesSize = Parameters.SIGNATURE_THRESHOLD;
    Signature[] certificates = new Signature[certificatesSize];
    for (int i = 0; i < certificatesSize; i++) {
      certificates[i] = SignatureFixture.newSignatureFixture();
    }
    return new ValidatedBlock(previousBlockId, proposer, transactions, signature, certificates, height);
  }
}
