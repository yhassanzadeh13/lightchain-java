package unittest.fixtures;

import java.util.ArrayList;
import java.util.Random;

import model.crypto.Signature;
import model.lightchain.*;
import org.junit.jupiter.api.Assertions;
import protocol.Parameters;

/**
 * A fixture for BlockFixture class.
 */
public class ValidatedBlockFixture {
  private static final Random random = new Random();

  /**
   * Returns a validated block with randomly generated values and given accounts of the block.
   *
   * @return a validated block with randomly generated values and given accounts of the block.
   */
  public static ValidatedBlock newValidatedBlock(ArrayList<Account> accounts) {
    Identifier previousBlockId = IdentifierFixture.newIdentifier();
    // first account is selected as identifier.
    Identifier proposer = accounts.get(0).getIdentifier();

    ValidatedTransaction[] transactions = ValidatedTransactionFixture.newValidatedTransactions(
        accounts,
        Parameters.MIN_TRANSACTIONS_NUM + 2);

    Signature signature = SignatureFixture.newSignatureFixture(proposer);
    int certificatesSize = Parameters.SIGNATURE_THRESHOLD;
    Signature[] certificates = new Signature[certificatesSize];
    for (int i = 0; i < certificatesSize; i++) {
      certificates[i] = SignatureFixture.newSignatureFixture();
    }
    int height = Math.abs(random.nextInt(1_000_000));

    ValidatedBlock validatedBlock =
        new ValidatedBlock(previousBlockId, proposer, transactions, signature, certificates, height);

    // identifier of a validated block should be equal to its core block structure
    Assertions.assertEquals(validatedBlock.id(), ((Block) validatedBlock).id());

    return validatedBlock;
  }
}