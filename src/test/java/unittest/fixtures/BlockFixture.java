package unittest.fixtures;

import model.crypto.Signature;
import model.lightchain.Block;
import model.lightchain.Identifier;
import model.lightchain.ValidatedTransaction;
import protocol.Parameters;

/**
 * Encapsulates creating random blocks for testing.
 */
public class BlockFixture {
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
    Signature signature = SignatureFixture.newSignatureFixture();
    return new Block(previousBlockId, proposer, transactions, signature);
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
    Signature signature = SignatureFixture.newSignatureFixture();
    return new Block(previousBlockId, proposer, transactions, signature);
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
    Signature signature = SignatureFixture.newSignatureFixture();
    return new Block(previousBlockId, proposer, transactions, signature);
  }
}
