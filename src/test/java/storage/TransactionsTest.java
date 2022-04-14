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
  // TODO: implement a unit test for each of the following scenarios:
  // IMPORTANT NOTE: each test must have a separate instance of database, and the database MUST only created on a
  // temporary directory.
  // In following tests by a "new" transaction, we mean transaction that already does not exist in the database,
  // and by a "duplicate" transaction, we mean one that already exists in the database.
  // 1. When adding 10 new transactions sequentially, the Add method must return true for all of them. Moreover, after
  //    adding transactions is done, querying the Has method for each of the transaction should return true. Also, when
  //    querying All method, list of all 10 transactions must be returned. Moreover, all transactions should be
  //    retrievable through get method.
  // 2. Repeat test case 1 for concurrently adding transactions as well as concurrently querying the database for has,
  //    and get.
  // 3. Add 10 new transactions, check that they are added correctly, i.e., while adding each transactions
  //    Add must return true, Has returns true for each of them, and All returns list of all of them, and get must
  //    return the transaction. Then Remove the first
  //    5 transactions. While Removing each of them, the Remove should return true.
  //    Then query all 10 transactions using Has. Has should return false for the first 5 transactions
  //    that have been removed, and get should return null for them. But for the last 5 transactions has
  //    should return true, and get should return the transaction. Also, All should return only the last 5 transactions.
  // 4. Repeat test case 3 for concurrently adding and removing transactions as well as concurrently querying the
  //    database for has and get.
  // 5. Add 10 new transactions and check that all of them are added correctly, i.e., while adding each transaction
  //    Add must return true, Has returns true for each of them, get should return the transaction,
  //    and All returns list of all of them. Then try Adding all of them again, and
  //    Add should return false for each of them,  while has should still return true, and get should be able to
  //    able to retrieve the transaction.
  // 6. Repeat test case 5 for concurrently adding transactions as well as concurrently querying the
  //    database for has, and get.

  /**
   * Set the tests up.
   */
  @BeforeEach
  void setUp() throws IOException {
    Path currentRelativePath = Paths.get("");
    tempdir = Files.createTempDirectory(currentRelativePath, TEMP_DIR);
    db = new TransactionsMapDb(tempdir.toAbsolutePath() + "/" + TEMP_FILE);
    allTransactions = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      allTransactions.add(TransactionFixture.newTransaction(10));
    }
  }

  /**
   * Adding transactions sequentially.
   *
   * @throws IOException throw IOException.
   */
  @Test
  void sequentialAddTest() throws IOException {
    for (Transaction transaction : allTransactions) {
      Assertions.assertTrue(db.add(transaction));
    }
    for (Transaction transaction : allTransactions) {
      Assertions.assertTrue(db.has(transaction.id()));
    }
    ArrayList<Transaction> all = db.all();
    Assertions.assertEquals(all.size(), 10);
    for (Transaction transaction : all) {
      Assertions.assertTrue(allTransactions.contains(transaction));
    }
    for (Transaction transaction : allTransactions) {
      Assertions.assertTrue(allTransactions.contains(db.get(transaction.id())));
    }
    db.closeDb();
    FileUtils.deleteDirectory(new File(tempdir.toString()));
  }

  @Test
  void concurrentAddTest() throws IOException {
    int concurrencyDegree = 10;

    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch addDone = new CountDownLatch(concurrencyDegree);
    Thread[] addThreads = new Thread[concurrencyDegree];
     /*
    Adding all transactions concurrently.
     */
    for (int i = 0; i < allTransactions.size(); i++) {
      int finalI = i;
      addThreads[i] = new Thread(() -> {
        if (!db.add(allTransactions.get(finalI))) {
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
     /*
    Checking correctness of insertion by Has.
     */
    CountDownLatch hasDone = new CountDownLatch(concurrencyDegree);
    Thread[] hasThreads = new Thread[concurrencyDegree];
    for (int i = 0; i < allTransactions.size(); i++) {
      int finalI = i;
      hasThreads[i] = new Thread(() -> {
        if (!db.has((allTransactions.get(finalI)).id())) {
          threadError.getAndIncrement();
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

     /*
    Checking correctness of insertion by GET.
     */
    CountDownLatch getDone = new CountDownLatch(concurrencyDegree);
    Thread[] getThreads = new Thread[concurrencyDegree];
    for (int i = 0; i < allTransactions.size(); i++) {
      int finalI = i;
      getThreads[i] = new Thread(() -> {
        if (!allTransactions.contains(db.get(allTransactions.get(finalI).id()))) {
          threadError.getAndIncrement();
        }
        getDone.countDown();
      });
    }

    for (Thread t : getThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = getDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    /*
    Retrieving all concurrently.
     */
    CountDownLatch doneAll = new CountDownLatch(concurrencyDegree);
    Thread[] allThreads = new Thread[concurrencyDegree];
    ArrayList<Transaction> all = db.all();
    for (int i = 0; i < all.size(); i++) {
      int finalI = i;
      allThreads[i] = new Thread(() -> {
        if (!all.contains(allTransactions.get(finalI))) {
          threadError.getAndIncrement();
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
    db.closeDb();
    FileUtils.deleteDirectory(new File(tempdir.toString()));
  }

  /**
   * Add 10 new transaction, remove first 5 and test methods.
   */
  @Test
  void removeFirstFiveTest() throws IOException {
    for (Transaction transaction : allTransactions) {
      Assertions.assertTrue(db.add(transaction));
    }
    for (int i = 0; i < 5; i++) {
      Assertions.assertTrue(db.remove(allTransactions.get(i).id()));
    }
    for (int i = 0; i < 10; i++) {
      if (i < 5) {
        Assertions.assertFalse(db.has(allTransactions.get(i).id()) || db.all().contains(allTransactions.get(i)));
      } else {
        Assertions.assertTrue(db.has(allTransactions.get(i).id()) && db.all().contains(allTransactions.get(i)));
      }
    }
    db.closeDb();
    FileUtils.deleteDirectory(new File(tempdir.toString()));
  }

  /**
   * Concurrent version of removeFirstFiveTest.
   */
  @Test
  void concurrentRemoveFirstFiveTest() throws IOException {
    int concurrencyDegree = 10;

    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch addDone = new CountDownLatch(concurrencyDegree);
    Thread[] addThreads = new Thread[concurrencyDegree];
     /*
    Adding all transactions concurrently.
     */
    for (int i = 0; i < allTransactions.size(); i++) {
      int finalI = i;
      addThreads[i] = new Thread(() -> {
        if (!db.add(allTransactions.get(finalI))) {
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
   /*
    Removing first 5 concurrently
     */
    int removeTill = concurrencyDegree / 2;
    CountDownLatch doneRemove = new CountDownLatch(removeTill);
    Thread[] removeThreads = new Thread[removeTill];
    for (int i = 0; i < removeTill; i++) {
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
    CountDownLatch doneHas = new CountDownLatch(concurrencyDegree);
    Thread[] hasThreads = new Thread[concurrencyDegree];
    for (int i = 0; i < allTransactions.size(); i++) {
      int finalI = i;
      int finalI1 = i;
      hasThreads[i] = new Thread(() -> {
        if (allTransactions.indexOf(allTransactions.get(finalI)) < 5) {
          if (db.has(allTransactions.get(finalI1).id())) {
            threadError.getAndIncrement();
          }
        } else {
          if (!db.has(allTransactions.get(finalI).id())) {
            threadError.getAndIncrement();
          }
        }
        doneHas.countDown();
      });
    }

    for (Thread t : hasThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = doneHas.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    CountDownLatch getDone = new CountDownLatch(concurrencyDegree / 2);
    Thread[] getThreads = new Thread[concurrencyDegree / 2];
    for (int i = 0; i < concurrencyDegree / 2; i++) {
      int finalI = i;
      int finalI1 = i + 5;
      getThreads[i] = new Thread(() -> {
        if (!allTransactions.contains(db.get(allTransactions.get(finalI).id()))
            || allTransactions.contains(db.get(allTransactions.get(finalI1).id()))) {
          threadError.getAndIncrement();
        }
        getDone.countDown();
      });
    }

    for (Thread t : getThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = getDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    db.closeDb();
    FileUtils.deleteDirectory(new File(tempdir.toString()));
  }

  /**
   * Add 10 identifiers already exist and return false expected.
   */
  @Test
  void duplicationTest() throws IOException {
   /*
   Firt part of the test
    */
    for (Transaction transaction : allTransactions) {
      Assertions.assertTrue(db.add(transaction));
    }
    for (Transaction transaction : allTransactions) {
      Assertions.assertTrue(db.has(transaction.id()));
    }
    ArrayList<Transaction> all = db.all();
    Assertions.assertEquals(all.size(), 10);
    for (Transaction transaction : all) {
      Assertions.assertTrue(allTransactions.contains(transaction));
    }
    for (Transaction transaction : allTransactions) {
      Assertions.assertTrue(allTransactions.contains(db.get(transaction.id())));
    }
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
    db.closeDb();
    FileUtils.deleteDirectory(new File(tempdir.toString()));
  }
  /**
   * Concurrent version of duplicationTest.
   */
  @Test
  void concurrentDuplicationTest() throws IOException {
    int concurrencyDegree = 10;

    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch addDone = new CountDownLatch(concurrencyDegree);
    Thread[] addThreads = new Thread[concurrencyDegree];
     /*
    Adding all transactions concurrently.
     */
    for (int i = 0; i < allTransactions.size(); i++) {
      int finalI = i;
      addThreads[i] = new Thread(() -> {
        if (!db.add(allTransactions.get(finalI))) {
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
     /*
    Checking correctness of insertion by Has.
     */
    CountDownLatch hasDone = new CountDownLatch(concurrencyDegree);
    Thread[] hasThreads = new Thread[concurrencyDegree];
    for (int i = 0; i < allTransactions.size(); i++) {
      int finalI = i;
      hasThreads[i] = new Thread(() -> {
        if (!db.has((allTransactions.get(finalI)).id())) {
          threadError.getAndIncrement();
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

     /*
    Checking correctness of insertion by Get.
     */
    CountDownLatch getDone = new CountDownLatch(concurrencyDegree);
    Thread[] getThreads = new Thread[concurrencyDegree];
    for (int i = 0; i < allTransactions.size(); i++) {
      int finalI = i;
      getThreads[i] = new Thread(() -> {
        if (!allTransactions.contains(db.get(allTransactions.get(finalI).id()))) {
          threadError.getAndIncrement();
        }
        getDone.countDown();
      });
    }

    for (Thread t : getThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = getDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    /*
    Retrieving all concurrently.
     */
    CountDownLatch doneAll = new CountDownLatch(concurrencyDegree);
    Thread[] allThreads = new Thread[concurrencyDegree];
    ArrayList<Transaction> all = db.all();
    for (int i = 0; i < all.size(); i++) {
      int finalI = i;
      allThreads[i] = new Thread(() -> {
        if (!all.contains(allTransactions.get(finalI))) {
          threadError.getAndIncrement();
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
    /*
    Adding existing transactions.
     */
    CountDownLatch addDuplicateDone = new CountDownLatch(concurrencyDegree);
    Thread[] addDuplicateThreads = new Thread[concurrencyDegree];
     /*
    Adding all transactions concurrently.
     */
    for (int i = 0; i < allTransactions.size(); i++) {
      int finalI = i;
      addDuplicateThreads[i] = new Thread(() -> {
        if (db.add(allTransactions.get(finalI))) {
          threadError.getAndIncrement();
        }
        addDuplicateDone.countDown();
      });
    }
    for (Thread t : addDuplicateThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = addDuplicateDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    /*
    Checking correctness of insertion by Has again.
     */
    CountDownLatch hasDone2 = new CountDownLatch(concurrencyDegree);
    Thread[] hasThreads2 = new Thread[concurrencyDegree];
    for (int i = 0; i < allTransactions.size(); i++) {
      int finalI = i;
      hasThreads2[i] = new Thread(() -> {
        if (!db.has((allTransactions.get(finalI)).id())) {
          threadError.getAndIncrement();
        }
        hasDone2.countDown();
      });
    }

    for (Thread t : hasThreads2) {
      t.start();
    }
    try {
      boolean doneOneTime = hasDone2.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }

     /*
    Checking correctness of insertion by Get again.
     */
    CountDownLatch getDone2 = new CountDownLatch(concurrencyDegree);
    Thread[] getThreads2 = new Thread[concurrencyDegree];
    for (int i = 0; i < allTransactions.size(); i++) {
      int finalI = i;
      getThreads2[i] = new Thread(() -> {
        if (!allTransactions.contains(db.get(allTransactions.get(finalI).id()))) {
          threadError.getAndIncrement();
        }
        getDone2.countDown();
      });
    }

    for (Thread t : getThreads2) {
      t.start();
    }
    try {
      boolean doneOneTime = getDone2.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    db.closeDb();
    FileUtils.deleteDirectory(new File(tempdir.toString()));
  }
}
