package protocol.transaction;

import model.lightchain.Transaction;

/**
 * Validator encapsulates the logic of a transaction validator in LightChain.
 */
public interface Validator {
  /**
   * Validates transaction parameters.
   *
   * @param transaction the transaction under validation.
   * @return true if all transaction fields have a valid value, and false otherwise. A transaction is valid if the
   * reference block id is a valid and finalized block, the sender and receiver both refer to valid accounts at the
   * snapshot of the reference block id, and the amount value is non-negative.
   */
  boolean isCorrect(Transaction transaction);

  /**
   * Validates the soundness of transaction based on LightChain protocol.
   *
   * @param transaction the transaction under validation.
   * @return true if reference block id of this transaction has a strictly higher height than the height of the
   * last block id in the sender account, and false otherwise.
   */
  boolean isSound(Transaction transaction);

  /**
   * Validates that the transaction is indeed issued by the sender.
   *
   * @param transaction the transaction under validation.
   * @return true if the transaction has a valid signature that is verifiable by the public key of its sender,
   * false otherwise.
   */
  boolean isAuthenticated(Transaction transaction);

  /**
   * Validates that sender has enough balance to cover the transaction.
   *
   * @param transaction the transaction under validation.
   * @return true if sender has a greater than or equal balance than the amount of transaction, and false otherwise.
   * The balance of sender must be checked at the snapshot of the reference block of the transaction.
   */
  boolean senderHasEnoughBalance(Transaction transaction);
}
