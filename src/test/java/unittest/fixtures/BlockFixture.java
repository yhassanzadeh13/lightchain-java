package unittest.fixtures;

import java.util.ArrayList;
import java.util.Random;

import model.crypto.Signature;
import model.lightchain.Account;
import model.lightchain.Block;
import model.lightchain.Identifier;
import model.lightchain.ValidatedTransaction;
import protocol.Parameters;

/**
 * Encapsulates creating random blocks for testing.
 */
public class BlockFixture {
  private static final Random random = new Random();

  /**
   * Returns a block with randomly generated values.
   *
   * @return a block with randomly generated values.
   */
  public static Block newBlock() {
    Identifier previousBlockId = IdentifierFixture.newIdentifier();
    Identifier proposer = IdentifierFixture.newIdentifier();
    int validatedTransactionsSize = Parameters.MIN_TRANSACTIONS_NUM + 2;
    ValidatedTransaction[] transactions = new ValidatedTransaction[validatedTransactionsSize];
    for (int i = 0; i < validatedTransactionsSize; i++) {
      transactions[i] = ValidatedTransactionFixture.newValidatedTransaction();
    }
    Signature signature = SignatureFixture.newSignatureFixture(proposer);

    int height = Math.abs(random.nextInt(1_000_000));
    return new Block(previousBlockId, proposer, height, transactions, signature);
  }

  /**
   * Returns a block with randomly generated values and given validated transactions size.
   *
   * @param validatedTransactionsSize size of the validated transactions array.
   * @return a block with randomly generated values.
   */
  public static Block newBlock(int validatedTransactionsSize) {
    Identifier previousBlockId = IdentifierFixture.newIdentifier();
    Identifier proposer = IdentifierFixture.newIdentifier();
    ValidatedTransaction[] transactions = new ValidatedTransaction[validatedTransactionsSize];
    for (int i = 0; i < validatedTransactionsSize; i++) {
      transactions[i] = ValidatedTransactionFixture.newValidatedTransaction();
    }
    Signature signature = SignatureFixture.newSignatureFixture(proposer);

    int height = Math.abs(random.nextInt(1_000_000));
    return new Block(previousBlockId, proposer, height, transactions, signature);
  }

  /**
   * Returns a block with randomly generated values and given validated transactions.
   *
   * @param transactions validated transactions array.
   * @return a block with randomly generated values.
   */
  public static Block newBlock(ValidatedTransaction[] transactions) {
    Identifier previousBlockId = IdentifierFixture.newIdentifier();
    Identifier proposer = IdentifierFixture.newIdentifier();
    Signature signature = SignatureFixture.newSignatureFixture(proposer);

    int height = Math.abs(random.nextInt(1_000_000));
    return new Block(previousBlockId, proposer, height, transactions, signature);
  }

  /**
   * Returns a block with randomly generated values and given previous block id.
   *
   * @param previousBlockId previous block id.
   * @param height          height of the block.
   * @return a block with randomly generated values.
   */
  public static Block newBlock(Identifier previousBlockId, int height) {
    Identifier proposer = IdentifierFixture.newIdentifier();
    int validatedTransactionsSize = Parameters.MIN_TRANSACTIONS_NUM + 2;
    ValidatedTransaction[] transactions = new ValidatedTransaction[validatedTransactionsSize];
    for (int i = 0; i < validatedTransactionsSize; i++) {
      transactions[i] = ValidatedTransactionFixture.newValidatedTransaction();
    }
    Signature signature = SignatureFixture.newSignatureFixture(proposer);

    return new Block(previousBlockId, proposer, height, transactions, signature);
  }

  /**
   * Returns a block with given proposer identifier, previous block id, block height, accounts list and
   * randomly generated values.
   *
   * @param proposer        proposer identifier.
   * @param previousBlockId previous block id.
   * @param height          height of the block.
   * @param accounts        accounts list to choose validated transaction senders/receivers from.
   * @return a block with randomly generated and given values.
   */
  public static Block newBlock(Identifier proposer, Identifier previousBlockId,
                               int height, ArrayList<Account> accounts) {
    int accountsSize = accounts.size();
    Identifier signer = accounts.get(random.nextInt(accountsSize)).getIdentifier();
    int validatedTransactionsSize = Parameters.MIN_TRANSACTIONS_NUM + 2;
    ValidatedTransaction[] transactions = new ValidatedTransaction[validatedTransactionsSize];
    for (int i = 0; i < validatedTransactionsSize; i++) {
      transactions[i] = ValidatedTransactionFixture.newValidatedTransaction(
              previousBlockId,
              accounts.get(i).getIdentifier(),
              accounts.get(i + 1).getIdentifier(),
              signer,
              accounts);
    }
    Signature signature = SignatureFixture.newSignatureFixture(proposer);
    return new Block(previousBlockId, proposer, height, transactions, signature);
  }

  /**
   * Returns a block with given proposer identifier, previous block id, block height, accounts list and
   * randomly generated values.
   *
   * @param proposer        proposer identifier.
   * @param previousBlockId previous block id.
   * @param height          height of the block.
   * @param accounts        accounts list to choose validated transaction senders/receivers from.
   * @return a block with randomly generated and given values.
   */
  public static Block newBlock(Identifier proposer, Identifier previousBlockId,
                               int height, ArrayList<Account> accounts,
                               int validatedTransactionsSize) {
    int accountsSize = accounts.size();
    Identifier signer = accounts.get(random.nextInt(accountsSize)).getIdentifier();
    ValidatedTransaction[] transactions = new ValidatedTransaction[validatedTransactionsSize];
    for (int i = 0; i < validatedTransactionsSize; i++) {
      transactions[i] = ValidatedTransactionFixture.newValidatedTransaction(
              previousBlockId,
              accounts.get(i).getIdentifier(),
              accounts.get(i + 1).getIdentifier(),
              signer,
              accounts);
    }
    Signature signature = SignatureFixture.newSignatureFixture(proposer);
    return new Block(previousBlockId, proposer, height, transactions, signature);
  }

  /**
   * Returns a block with given proposer identifier, previous block id, block height, accounts list and
   * randomly generated values which has a transaction list that are not all validated.
   *
   * @param proposer        proposer identifier.
   * @param previousBlockId previous block id.
   * @param height          height of the block.
   * @param accounts        accounts list to choose validated transaction senders/receivers from.
   * @return a block with randomly generated and given values.
   */
  public static Block newBlockUnvalidatedTransaction(Identifier proposer, Identifier previousBlockId,
                                                     int height, ArrayList<Account> accounts) {
    int accountsSize = accounts.size();
    Identifier signer = accounts.get(random.nextInt(accountsSize)).getIdentifier();
    int validatedTransactionsSize = Parameters.MIN_TRANSACTIONS_NUM + 2;
    ValidatedTransaction[] transactions = new ValidatedTransaction[validatedTransactionsSize];
    for (int i = 0; i < validatedTransactionsSize; i++) {
      transactions[i] = ValidatedTransactionFixture.newValidatedTransaction(
              previousBlockId,
              accounts.get(i).getIdentifier(),
              accounts.get(i + 1).getIdentifier(),
              signer,
              accounts);
    }
    transactions[validatedTransactionsSize - 2] = ValidatedTransactionFixture.newValidatedTransaction(previousBlockId,
            accounts.get(validatedTransactionsSize - 2).getIdentifier(),
            accounts.get(validatedTransactionsSize - 1).getIdentifier(), signer, accounts, 5);
    Signature signature = SignatureFixture.newSignatureFixture(proposer);
    return new Block(previousBlockId, proposer, height, transactions, signature);
  }

  /**
   * Returns a block with given proposer identifier, previous block id, block height, accounts list and
   * randomly generated values.
   *
   * @param proposer        proposer identifier.
   * @param previousBlockId previous block id.
   * @param height          height of the block.
   * @param accounts        accounts list to choose validated transaction senders/receivers from.
   * @return a block with randomly generated and given values.
   */
  public static Block newBlockDuplicateSender(Identifier proposer, Identifier previousBlockId,
                                              int height, ArrayList<Account> accounts) {
    int accountsSize = accounts.size();
    Identifier signer = accounts.get(random.nextInt(accountsSize)).getIdentifier();
    int validatedTransactionsSize = Parameters.MIN_TRANSACTIONS_NUM + 2;
    ValidatedTransaction[] transactions = new ValidatedTransaction[validatedTransactionsSize];
    for (int i = 0; i < validatedTransactionsSize - 1; i++) {
      transactions[i] = ValidatedTransactionFixture.newValidatedTransaction(
              previousBlockId,
              accounts.get(i).getIdentifier(),
              accounts.get(i + 1).getIdentifier(),
              signer,
              accounts);
    }
    transactions[validatedTransactionsSize - 1] = ValidatedTransactionFixture.newValidatedTransaction(
            previousBlockId,
            accounts.get(validatedTransactionsSize - 2).getIdentifier(),
            accounts.get(validatedTransactionsSize - 1).getIdentifier(),
            signer,
            accounts);
    Signature signature = SignatureFixture.newSignatureFixture(proposer);
    return new Block(previousBlockId, proposer, height, transactions, signature);
  }
}