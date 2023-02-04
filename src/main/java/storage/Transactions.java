package storage;

import java.util.ArrayList;

import model.lightchain.Identifier;
import model.lightchain.Transaction;

/**
 * Represents a persistent key-value store for transactions on disk.
 */
public interface Transactions {
  /**
   * Checks existence of a transaction on the database.
   *
   * @param transactionId Identifier of transaction.
   * @return true if a transaction with that identifier exists, false otherwise.
   */
  boolean has(Identifier transactionId);

  /**
   * Adds transaction to the database.
   *
   * @param transaction given transaction to be added.
   * @return true if transaction did not exist on the database, false if transaction is already in
   *     database.
   */
  boolean add(Transaction transaction);

  /**
   * Removes transaction with given identifier.
   *
   * @param transactionId identifier of the transaction.
   * @return true if transaction exists on database and removed successfully, false if transaction does not exist on
   *     database.
   */
  boolean remove(Identifier transactionId);

  /**
   * Returns the transaction with given identifier.
   *
   * @param transactionId identifier of the transaction.
   * @return the transaction itself if exists and null otherwise.
   */
  Transaction get(Identifier transactionId);

  /**
   * Returns all transactions stored in database.
   *
   * @return all transactions stored tranin database.
   */
  ArrayList<Transaction> all();

  /**
   * Returns the size of all transactions stored in database.
   *
   * @return the size of all transactions stored in database.
   */
  int size();
}