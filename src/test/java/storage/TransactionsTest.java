package storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import model.lightchain.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import storage.mapdb.TransactionsMapDb;
import unittest.fixtures.TransactionFixture;

/**
 * Encapsulates tests for transactions database.
 */
public class TransactionsTest {

  private static final String TEMP_DIR = "tempdir";
  private static final String TEMP_FILE = "tempfile.db";
  private Path tempdir;
  private ArrayList<Transaction> allTransactions;
  private TransactionsMapDb db;

  /**
   * Initializes database.
   */
  @BeforeEach
  void setup() throws IOException {
    Path currentRelativePath = Paths.get("");
    tempdir = Files.createTempDirectory(currentRelativePath, TEMP_DIR);
    db = new TransactionsMapDb(tempdir.toAbsolutePath() + "/" + TEMP_FILE);
    allTransactions = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      allTransactions.add(TransactionFixture.newTransaction(10));
    }
  }

  /**
   * Closes database.
   */
  @AfterEach
  void cleanup() throws IOException {
    db.closeDb();
    FileUtils.deleteDirectory(new File(tempdir.toString()));
  }

  /**
   * When adding 10 new transactions sequentially, the Add method must return true for all of them. Moreover, after
   * adding transactions is done, querying the Has method for each of the transaction should return true. Also, when
   * querying All method, list of all 10 transactions must be returned. Moreover, all transactions should be
   * retrievable through get method.
   *
   */
  @Test
  void sequentialAddTest() {
    for (Transaction transaction : allTransactions) {
      Assertions.assertTrue(db.add(transaction));
    }
    for (Transaction transaction : allTransactions) {
      Assertions.assertTrue(db.has(transaction.id()));
    }
    for (Transaction transaction : allTransactions) {
      Assertions.assertEquals(transaction, db.get(transaction.id()));
    }
    ArrayList<Transaction> all = db.all();
    Assertions.assertEquals(all.size(), 10);
    for (Transaction transaction : all) {
      Assertions.assertTrue(allTransactions.contains(transaction));
    }
  }

  /**
   * When adding 10 new transactions concurrently, the Add method must return true for all of them. Moreover, after
   * adding transactions is done, querying the Has method for each of the transaction should return true. Also, when
   * querying All method, list of all 10 transactions must be returned. Moreover, all transactions should be
   * retrievable through get method.
   */
  @Test
  void concurrentAddTest() {
    /*
    Adding all transactions concurrently.
    */
    this.addAllTransactionsConcurrently(true);

    /*
    All blocks should be retrievable
    */
    this.checkHasConcurrently(0);
    this.checkForGetConcurrently(0);
    this.checkForAllConcurrently(0);
  }

  /**
   * Add 10 new transactions SEQUENTIALLY, check that they are added correctly, i.e., while adding each transaction
   * Add must return
   * true, Has returns true for each of them, each transaction is retrievable by identifier,
   * and All returns list of all of them. Then Remove the first 5 transactions sequentially.
   * While Removing each of them, the Remove should return true. Then query all 10 transactions using has, get.
   */
  @Test
  void removeFirstFiveTest()  {
    for (Transaction transaction : allTransactions) {
      Assertions.assertTrue(db.add(transaction));
    }
    for (Transaction transaction : allTransactions) {
      Assertions.assertTrue(db.has(transaction.id()));
    }
    for (Transaction transaction : allTransactions) {
      Assertions.assertEquals(transaction, db.get(transaction.id()));
    }

    ArrayList<Transaction> all = db.all();
    Assertions.assertEquals(all.size(), 10);
    for (Transaction transaction : all) {
      Assertions.assertTrue(allTransactions.contains(transaction));
    }

    // removing first five
    for (int i = 0; i < 5; i++) {
      Assertions.assertTrue(db.remove(allTransactions.get(i).id()));
    }
    for (int i = 0; i < 10; i++) {
      Transaction transaction = allTransactions.get(i);
      if (i < 5) {
        Assertions.assertFalse(db.has(transaction.id()));
        Assertions.assertFalse(db.all().contains(transaction));
        Assertions.assertNull(db.get(transaction.id()));
      } else {
        Assertions.assertTrue(db.has(transaction.id()));
        Assertions.assertTrue(db.all().contains(transaction));
        Assertions.assertEquals(transaction, db.get(transaction.id()));
      }
    }
  }

  /**
   * Add 10 new transactions CONCURRENTLY, check that they are added correctly, i.e., while adding each transaction
   * Add must return
   * true, Has returns true for each of them, each transaction is retrievable by identifier,
   * and All returns list of all of them. Then Remove the first 5 transactions sequentially.
   * While Removing each of them, the Remove should return true. Then query all 10 transactions using has, get.
   */
  @Test
  void concurrentRemoveFirstFiveTest() {

    /*
    Adding all transactions concurrently.
    */
    this.addAllTransactionsConcurrently(true);

    /*
    All transactions should be retrievable using their id or height.
    */
    this.checkForGetConcurrently(0);
    this.checkHasConcurrently(0);
    this.checkForAllConcurrently(0);

    /*
    Removing first 5 concurrently
     */
    this.removeBlocksTill(5);

    /*
    first five transactions must not be retrievable,
    the rest must be.
     */
    this.checkForGetConcurrently(5);
    this.checkHasConcurrently(5);
    this.checkForAllConcurrently(5);
  }

  /**
   * Add 10 new transactions SEQUENTIALLY and check that all of them are added correctly, i.e., while adding
   * each transaction
   * Add must return true, has returns true for each of them, and All returns list of all of them. Moreover, each
   * transaction is retrievable using get.
   * Then try Adding all of them again, and Add should return false for each of them,
   * while has should still return true, and get should be
   * able to retrieve the transaction.
   */
  @Test
  void duplicationTest() {
    for (Transaction transaction : allTransactions) {
      Assertions.assertTrue(db.add(transaction));
    }
    for (Transaction transaction : allTransactions) {
      Assertions.assertTrue(db.has(transaction.id()));
    }
    for (Transaction transaction : allTransactions) {
      Assertions.assertEquals(transaction, db.get(transaction.id()));
    }
    ArrayList<Transaction> all = db.all();
    Assertions.assertEquals(all.size(), 10);
    for (Transaction transaction : all) {
      Assertions.assertTrue(allTransactions.contains(transaction));
    }
    for (Transaction transaction : allTransactions) {
      Assertions.assertTrue(allTransactions.contains(db.get(transaction.id())));
    }

    // adding all again
    for (Transaction transaction : allTransactions) {
      Assertions.assertFalse(db.add(transaction));
    }
    /*
    After trying duplication, check again.
    */
    for (Transaction transaction : allTransactions) {
      Assertions.assertTrue(db.has(transaction.id()));
    }
    for (Transaction transaction : allTransactions) {
      Assertions.assertTrue(allTransactions.contains(db.get(transaction.id())));
    }
  }

  /**
   * Add 10 new transactions CONCURRENTLY and check that all of them are added correctly, i.e., while adding
   * each transaction
   * Add must return true, has returns true for each of them, and All returns list of all of them. Moreover, each
   * transaction is retrievable using get.
   * Then try Adding all of them again, and Add should return false for each of them,
   * while has should still return true, and get should be
   * able to retrieve the transaction.
   */
  @Test
  void concurrentDuplicationTest() {
    /*
    Adding all transactions concurrently.
     */
    this.addAllTransactionsConcurrently(true);

    /*
    All transactions should be retrievable using their id or height.
    */
    this.checkForGetConcurrently(0);
    this.checkHasConcurrently(0);
    this.checkForAllConcurrently(0);

    /*
    Adding all transactions again concurrently, all should fail due to duplication.
     */
    this.addAllTransactionsConcurrently(false);

    /*
    Again, all transactions should be retrievable using their id or height.
    */
    this.checkHasConcurrently(0);
    this.checkForGetConcurrently(0);
    this.checkForAllConcurrently(0);
  }

  /**
   * Adds all transactions to the transaction storage.
   *
   * @param expectedResult expected boolean result after each insertion; true means transaction added successfully,
   *                       false means transaction was not added successfully.
   */
  private void addAllTransactionsConcurrently(boolean expectedResult) {
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch addDone = new CountDownLatch(allTransactions.size());
    Thread[] addThreads = new Thread[allTransactions.size()];
    /*
    Adding all transactions concurrently.
     */
    for (int i = 0; i < allTransactions.size(); i++) {
      int finalI = i;
      addThreads[i] = new Thread(() -> {
        if (db.add(allTransactions.get(finalI)) != expectedResult) {
          threadError.getAndIncrement();
        }
        addDone.countDown();
      });
    }
    for (Thread t : addThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = addDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    Assertions.assertEquals(0, threadError.get());
  }

  /**
   * Checks existence of transactions in the transactions storage database starting from the given index.
   *
   * @param from inclusive index of the first transactions to check.
   */
  private void checkHasConcurrently(int from) {
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch hasDone = new CountDownLatch(allTransactions.size());
    Thread[] hasThreads = new Thread[allTransactions.size()];
    for (int i = 0; i < allTransactions.size(); i++) {
      int finalI = i;
      Transaction transaction = allTransactions.get(i);
      hasThreads[i] = new Thread(() -> {

        if (finalI < from) {
          // transaction should not exist
          if (this.db.has(transaction.id())) {
            threadError.incrementAndGet();
          }
        } else {
          // transaction should exist
          if (!this.db.has(transaction.id())) {
            threadError.getAndIncrement();
          }
        }
        hasDone.countDown();
      });
    }
    for (Thread t : hasThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = hasDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    Assertions.assertEquals(0, threadError.get());
  }

  /**
   * Checks retrievability of transactions from the transaction storage database starting from the given index.
   *
   * @param from inclusive index of the first transaction to check.
   */
  private void checkForGetConcurrently(int from) {
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch getDone = new CountDownLatch(allTransactions.size());
    Thread[] getThreads = new Thread[allTransactions.size()];

    for (int i = 0; i < allTransactions.size(); i++) {
      int finalI = i;
      Transaction transaction = allTransactions.get(i);
      getThreads[i] = new Thread(() -> {
        Transaction got = db.get(transaction.id());
        if (finalI < from) {
          // transaction should not exist
          if (got != null) {
            threadError.incrementAndGet();
          }
        } else {
          // transaction should exist
          if (!transaction.equals(got)) {
            threadError.getAndIncrement();
          }
          if (!transaction.id().equals(got.id())) {
            threadError.getAndIncrement();
          }
        }

        getDone.countDown();
      });
    }

    for (Thread t: getThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = getDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    Assertions.assertEquals(0, threadError.get());
  }

  /**
   * Checks retrievability of transactions from the transaction storage database starting from the given index.
   *
   * @param from inclusive index of the first transaction to check.
   */
  private void checkForAllConcurrently(int from) {
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch doneAll = new CountDownLatch(allTransactions.size());
    Thread[] allThreads = new Thread[allTransactions.size()];
    ArrayList<Transaction> all = db.all();

    for (int i = 0; i < allTransactions.size(); i++) {
      int finalI = i;
      final Transaction transaction = allTransactions.get(i);
      allThreads[i] = new Thread(() -> {
        if (finalI < from) {
          // transaction should not exist
          if (all.contains(transaction)) {
            threadError.incrementAndGet();
          }
        } else {
          // transaction should exist
          if (!all.contains(transaction)) {
            threadError.getAndIncrement();
          }
        }
        doneAll.countDown();
      });
    }

    for (Thread t : allThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = doneAll.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    Assertions.assertEquals(0, threadError.get());
  }

  /**
   * Removes transactions from blocks transaction database till the given index concurrently.
   *
   * @param till exclusive index of the last transaction being removed.
   */
  private void removeBlocksTill(int till) {
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch doneRemove = new CountDownLatch(till);
    Thread[] removeThreads = new Thread[till];

    for (int i = 0; i < till; i++) {
      int finalI = i;
      removeThreads[i] = new Thread(() -> {
        if (!db.remove(allTransactions.get(finalI).id())) {
          threadError.getAndIncrement();
        }
        doneRemove.countDown();
      });
    }

    for (Thread t : removeThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = doneRemove.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    Assertions.assertEquals(0, threadError.get());
  }
}
