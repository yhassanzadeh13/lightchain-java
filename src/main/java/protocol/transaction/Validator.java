package protocol.transaction;

import model.lightchain.Transaction;

public interface Validator {
  boolean isCorrect(Transaction transaction);
  boolean isSound(Transaction transaction);
  boolean isAuthenticated(Transaction transaction);
  boolean senderHasEnoughBalance(Transaction transaction);
}
