package protocol.transaction;

import model.crypto.PublicKey;
import model.crypto.Signature;
import model.lightchain.Account;
import model.lightchain.Identifier;
import model.lightchain.Transaction;
import state.Snapshot;
import state.State;

public class TransactionVerifier implements Validator {
  private final State state;

  public TransactionVerifier(State state) {
    this.state = state;
  }

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

    return transaction.getAmount() > 0 ;
    // Amount is not positive.
  }

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

  @Override
  public boolean isAuthenticated(Transaction transaction) {
    Identifier referenceBlockID = transaction.getReferenceBlockId();
    Snapshot snapshot = state.atBlockId(referenceBlockID);

    Identifier sender = transaction.getSender();
    Account senderAccount = snapshot.getAccount(sender);
    PublicKey publicKey = senderAccount.getPublicKey();

    Signature signature = transaction.getSignature();
    return publicKey.verifySignature(transaction,signature);
  }

  @Override
  public boolean senderHasEnoughBalance(Transaction transaction) {
    Identifier referenceBlockID = transaction.getReferenceBlockId();
    Snapshot snapshot = state.atBlockId(referenceBlockID);

    Identifier sender = transaction.getSender();
    Account senderAccount = snapshot.getAccount(sender);
    double balance = senderAccount.getBalance();
    double amount = transaction.getAmount();

    return balance >= amount;
  }
}
