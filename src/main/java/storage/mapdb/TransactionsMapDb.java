package storage.mapdb;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import model.lightchain.Identifier;
import model.lightchain.Transaction;
import org.mapdb.*;
import storage.Transactions;

/**
 * Implementation of Transactions interface.
 */
public class TransactionsMapDb implements Transactions {
  private final DB db;
  private final ReentrantReadWriteLock lock;
  private static final String MAP_NAME = "transactions_map";
  private final HTreeMap transactionsMap;

  /**
   * Creates TransactionsMapDb.
   *
   * @param filePath the path of the file.
   */
  public TransactionsMapDb(String filePath) {
    this.db = DBMaker.fileDB(filePath).make();
    this.lock = new ReentrantReadWriteLock();
    transactionsMap = this.db.hashMap(MAP_NAME)
        .keySerializer(Serializer.BYTE_ARRAY)
        .createOrOpen();
  }

  /**
   * Checks existence of a transaction on the database.
   *
   * @param transactionId Identifier of transaction.
   * @return true if a transaction with that identifier exists, false otherwise.
   */
  @Override
  public boolean has(Identifier transactionId) {
    boolean hasBoolean;
    try {
      lock.readLock().lock();
      hasBoolean = transactionsMap.containsKey(transactionId.getBytes());
    } finally {
      lock.readLock().unlock();
    }
    return hasBoolean;
  }

  /**
   * Adds transaction to the database.
   *
   * @param transaction given transaction to be added.
   * @return true if transaction did not exist on the database, false if transaction is already in
   * database.
   */
  @Override
  public boolean add(Transaction transaction) {
    boolean addBoolean;
    try {
      lock.writeLock().lock();
      addBoolean = transactionsMap.putIfAbsentBoolean(transaction.id().getBytes(), transaction);
    } finally {
      lock.writeLock().unlock();
    }
    return addBoolean;
  }

  /**
   * Removes transaction with given identifier.
   *
   * @param transactionId identifier of the transaction.
   * @return true if transaction exists on database and removed successfully, false if transaction does not exist on
   * database.
   */
  @Override
  public boolean remove(Identifier transactionId) {
    boolean removeBoolean;
    try {
      lock.writeLock().lock();
      Transaction transaction = get(transactionId);
      removeBoolean = transactionsMap.remove(transactionId.getBytes(), transaction);
    } finally {
      lock.writeLock().unlock();
    }
    return removeBoolean;
  }

  /**
   * Returns the transaction with given identifier.
   *
   * @param transactionId identifier of the transaction.
   * @return the transaction itself if exists and null otherwise.
   */
  @Override
  public Transaction get(Identifier transactionId) {

    lock.readLock().lock();
    Transaction transaction = (Transaction) transactionsMap.get(transactionId.getBytes());
    lock.readLock().unlock();
    return transaction;
  }

  /**
   * Returns all transactions stored in database.
   *
   * @return all transactions stored tranin database.
   */
  @Override
  public ArrayList<Transaction> all() {
    ArrayList<Transaction> allTransactions = new ArrayList<>();
    for (Object transaction : transactionsMap.values()) {
      allTransactions.add((Transaction) transaction);
    }
    return allTransactions;
  }

  @Override
  public int size() {
    return transactionsMap.size();
  }

  /**
   * It closes the database.
   */
  public void closeDb() {
    db.close();
  }
}
