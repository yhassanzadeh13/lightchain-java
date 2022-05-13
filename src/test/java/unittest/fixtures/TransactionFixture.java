package unittest.fixtures;

import java.util.Random;

import model.crypto.Signature;
import model.lightchain.Identifier;
import model.lightchain.Transaction;

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
    Signature signature = SignatureFixture.newSignatureFixture();
    Transaction tx = new model.lightchain.Transaction(referenceBlockId, sender, receiver, amount);
    tx.setSignature(signature);
    return tx;
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
    Signature signature = SignatureFixture.newSignatureFixture();
    Transaction tx = new model.lightchain.Transaction(referenceBlockId, sender, receiver, amount);
    tx.setSignature(signature);
    return tx;
  }

  /**
   * Creates a new transaction with given reference block id, sender identifier, receiver identifier,
   * and randomly created transaction amount.
   *
   * @param referenceBlockId reference block id for the random Transaction object
   * @param sender           sender identifier for the random Transaction object
   * @param receiver         receiver identifier for the random Transaction object
   * @param signerId         signer identifier for the random Transaction object
   * @return random Transaction object
   */
  public static Transaction newTransaction(Identifier referenceBlockId, Identifier sender, Identifier receiver, Identifier signerId) {
    double amount = Math.abs(RANDOM.nextInt()) + 1;
    Signature signature = SignatureFixture.newSignatureFixture(signerId);
    Transaction tx = new model.lightchain.Transaction(referenceBlockId, sender, receiver, amount);
    tx.setSignature(signature);
    return tx;
  }

  /**
   * Creates a new transaction with given reference block id, sender identifier, receiver identifier,
   * and transaction amount.
   *
   * @param referenceBlockId reference block id for the random Transaction object
   * @param sender           sender identifier for the random Transaction object
   * @param receiver         receiver identifier for the random Transaction object
   * @param signerId         signer identifier for the random Transaction object
   * @param amount           amount sent for the random Transaction object
   * @return random Transaction object
   */
  public static Transaction newTransaction(Identifier referenceBlockId, Identifier sender, Identifier receiver, Identifier signerId, double amount) {
    Signature signature = SignatureFixture.newSignatureFixture(signerId);
    Transaction tx = new model.lightchain.Transaction(referenceBlockId, sender, receiver, amount);
    tx.setSignature(signature);
    return tx;
  }
}
