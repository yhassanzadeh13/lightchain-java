package storage;

import java.util.ArrayList;

import model.lightchain.Identifier;
import model.lightchain.Transaction;

/**
 * Represents a persistent key-value store for transactions on disk.
 */
public interface Transactions {
  boolean has(Identifier transactionId);

  boolean add(Transaction transaction);

  boolean remove(Identifier transactionId);

  Transaction get(Identifier transactionId);

  ArrayList<Transaction> all();
}
