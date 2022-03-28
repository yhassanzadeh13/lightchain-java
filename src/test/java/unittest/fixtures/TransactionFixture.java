package unittest.fixtures;

import java.util.Random;

import model.lightchain.Identifier;

/**
 * Encapsulates test utilities for transactions of LightChain.
 */
public class TransactionFixture {
  /**
   * Random object to create random amounts.
   */
  private static final Random RANDOM = new Random();

  /**
   * Creates a new transaction with randomly created sender identifier, receiver identifier, reference block id,
   * and given transaction amount.
   *
   * @param amount Transaction amount for the random Transaction object
   * @return random Transaction object
   */
  public static model.lightchain.Transaction newTransaction(double amount) {
    Identifier sender = IdentifierFixture.newIdentifier();
    Identifier receiver = IdentifierFixture.newIdentifier();
    Identifier referenceBlockId = IdentifierFixture.newIdentifier();
    return new model.lightchain.Transaction(sender, receiver, referenceBlockId, amount);
  }

  /**
   * Creates a new transaction with randomly created transaction amount, receiver identifier, reference block id,
   * and given sender identifier.
   *
   * @param sender sender identifier for the random Transaction object
   * @return random Transaction object
   */
  public static model.lightchain.Transaction newTransaction(Identifier sender) {
    Identifier receiver = IdentifierFixture.newIdentifier();
    Identifier referenceBlockId = IdentifierFixture.newIdentifier();
    double amount = Math.abs(RANDOM.nextInt()) + 1;
    return new model.lightchain.Transaction(sender, receiver, referenceBlockId, amount);
  }

}
