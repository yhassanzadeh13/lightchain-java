package protocol.transaction;

import model.lightchain.Account;
import model.lightchain.Identifier;
import model.lightchain.Transaction;
import state.Snapshot;
import state.State;

/**
 * Represents a verifier class that is used to verify a Transaction is valid.
 */
public class TransactionVerifier implements Validator {
  /**
   * Unique State that the transaction is in.
   */
  private final State state;

  /**
   * Constructor of a TransactionVerifier.
   *
   * @param state Current state used in the verifier.
   */
  public TransactionVerifier(State state) {
    this.state = state;
  }

  /**
   * Validates transaction parameters.
   *
   * @param transaction the transaction under validation.
   * @return true if all transaction fields have a valid value, and false otherwise. A transaction is valid if the
   * reference block id is a valid and finalized block, the sender and receiver both refer to valid accounts at the
   * snapshot of the reference block id, and the amount value is non-negative.
   */
  @Override
  public boolean isCorrect(Transaction transaction) {
    Identifier referenceBlockId = transaction.getReferenceBlockId();
    Snapshot snapshot = state.atBlockId(referenceBlockId);
    if (snapshot == null) {
      // Reference block id does not represent a valid snapshot.
      return false;
    }

    Account sender = snapshot.getAccount(transaction.getSender());
    if (sender == null) {
      // Sender account does not exist.
      return false;
    }

    Account receiver = snapshot.getAccount(transaction.getReceiver());
    if (receiver == null) {
      // Receiver account does not exist.
      return false;
    }

    return transaction.getAmount() > 0;
  }

  /**
   * Validates the soundness of transaction based on LightChain protocol.
   *
   * @param transaction the transaction under validation.
   * @return true if reference block id of this transaction has a strictly higher height than the height of the
   * last block id in the sender account, and false otherwise.
   */
  @Override
  public boolean isSound(Transaction transaction) {
    Identifier referenceBlockId = transaction.getReferenceBlockId();
    Snapshot snapshot = state.atBlockId(referenceBlockId);
    long referenceBlockHeight = snapshot.getReferenceBlockHeight();

    Account sender = snapshot.getAccount(transaction.getSender());
    Identifier lastBlockId = sender.getLastBlockId();
    Snapshot lastBlockSnapshot = state.atBlockId(lastBlockId);
    long lastBlockHeight = lastBlockSnapshot.getReferenceBlockHeight();

    return referenceBlockHeight > lastBlockHeight;
  }

  /**
   * Validates that the transaction is indeed issued by the sender.
   *
   * @param transaction the transaction under validation.
   * @return true if the transaction has a valid signature that is verifiable by the public key of its sender,
   * false otherwise.
   */
  @Override
  public boolean isAuthenticated(Transaction transaction) {
    return state.atBlockId(transaction.getReferenceBlockId())
        .getAccount(transaction.getSender())
        .getPublicKey()
        .verifySignature(transaction, transaction.getSignature());
  }

  /**
   * Validates that sender has enough balance to cover the transaction.
   *
   * @param transaction the transaction under validation.
   * @return true if sender has a greater than or equal balance than the amount of transaction, and false otherwise.
   * The balance of sender must be checked at the snapshot of the reference block of the transaction.
   */
  @Override
  public boolean senderHasEnoughBalance(Transaction transaction) {
    Snapshot snapshot = state.atBlockId(transaction.getReferenceBlockId());
    Account senderAccount = snapshot.getAccount(transaction.getSender());

    return senderAccount.getBalance() >= transaction.getAmount();
  }
}
