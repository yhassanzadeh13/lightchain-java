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

import model.lightchain.Identifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import storage.mapdb.IdentifierMapDb;
import unittest.fixtures.IdentifierFixture;

/**
 * Encapsulates tests for identifiers database.
 */
public class IdentifiersTest {

  private static final String TEMP_DIR = "tempdir";
  private static final String TEMP_FILE = "tempfile.db";
  private Path tempdir;
  private ArrayList<Identifier> allIds;
  private IdentifierMapDb db;
  // TODO: implement a unit test for each of the following scenarios:
  // IMPORTANT NOTE: each test must have a separate instance of database, and the database MUST only created on a
  // temporary directory.
  // In following tests by a "new" identifier, we mean an identifier that already does not exist in the database,
  // and by a "duplicate" identifier, we mean one that already exists in the database.
  // 1. When adding 10 new identifiers sequentially, the Add method must return true for all of them. Moreover, after
  //    adding identifiers is done, querying the Has method for each of the identifiers should return true. Also, when
  //    querying All method, list of all 10 identifiers must be returned.
  // 2. Add 10 new identifiers, check that they are added correctly, i.e., while adding each identifier Add must return
  //    true, Has returns true for each of them, and All returns list of all of them. Then Remove the first
  //    5 identifiers.
  //    While Removing each of them, the Remove should return true. Then query all 10 identifiers using Has.
  //    Has should return false for the first 5 identifiers that have been removed. But for the last 5 identifiers it
  //    should return true. Also, All should return only the last 5 identifiers.
  // 3. Add 10 new identifiers and check that all of them are added correctly, i.e., while adding each identifier
  //    Add must return true,
  //    Has returns true for each of them, and All returns list of all of them. Then try Adding all of them again, and
  //    Add should return false for each of them.

  /**
   * Set the tests up.
   */
  @BeforeEach
  void setUp() throws IOException {
    Path currentRelativePath = Paths.get("");
    tempdir = Files.createTempDirectory(currentRelativePath, TEMP_DIR);
    db = new IdentifierMapDb(tempdir.toAbsolutePath() + "/" + TEMP_FILE);
    allIds = IdentifierFixture.newIdentifiers(10);
  }

  /**
   * When adding 10 new identifiers sequentially, the Add method must return true for all of them.
   */
  @Test
  void sequentialAddTest() throws IOException {
    for (Identifier identifier : allIds) {
      Assertions.assertTrue(db.add(identifier));
    }
    for (Identifier identifier : allIds) {
      Assertions.assertTrue(db.has(identifier));
    }

    ArrayList<Identifier> all = db.all();
    Assertions.assertEquals(all.size(), 10);
    for (Identifier identifier : all) {
      Assertions.assertTrue(allIds.contains(identifier));
    }
    db.closeDb();
    FileUtils.deleteDirectory(new File(tempdir.toString()));
  }

  /**
   * Concurrent version of sequentialAddTest.
   */
  @Test
  void concurrentAddTest() throws IOException {
    int concurrencyDegree = 10;

    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch addDone = new CountDownLatch(concurrencyDegree);
    Thread[] addThreads = new Thread[concurrencyDegree];

    /*
    Adding all identifiers concurrently.
     */
    for (int i = 0; i < allIds.size(); i++) {
      int finalI = i;
      addThreads[i] = new Thread(() -> {
        if (!db.add(allIds.get(finalI))) {
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
    for (int i = 0; i < allIds.size(); i++) {
      int finalI = i;
      hasThreads[i] = new Thread(() -> {
        if (!db.has(allIds.get(finalI))) {
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
    Retrieving all concurrently.
     */
    CountDownLatch doneAll = new CountDownLatch(concurrencyDegree);
    Thread[] allThreads = new Thread[concurrencyDegree];
    ArrayList<Identifier> all = db.all();

    for (int i = 0; i < all.size(); i++) {
      int finalI = i;
      allThreads[i] = new Thread(() -> {
        if (!allIds.contains(allIds.get(finalI))) {
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
   * Add 10 new identifiers, check that they are added correctly, i.e., while adding each identifier Add must return.
   * true, Has returns true for each of them, and All returns list of all of them. Then Remove the first 5 identifiers.
   * While Removing each of them, the Remove should return true. Then query all 10 identifiers using Has.
   * Has should return false for the first 5 identifiers that have been removed. But for the last 5 identifiers it.
   * should return true. Also, All should return only the last 5 identifiers.
   */
  @Test
  void removeFirstFiveTest() throws IOException {

    for (Identifier identifier : allIds) {
      Assertions.assertTrue(db.add(identifier));
    }
    // removes the first five
    for (int i = 0; i < 5; i++) {
      Assertions.assertTrue(db.remove(allIds.get(i)));
    }
    for (int i = 0; i < 10; i++) {
      if (i < 5) {
        Assertions.assertFalse(db.has(allIds.get(i)) || db.all().contains(allIds.get(i)));
      } else {
        Assertions.assertTrue(db.has(allIds.get(i)) && db.all().contains(allIds.get(i)));
      }
    }
    db.closeDb();
    FileUtils.deleteDirectory(new File(tempdir.toString()));
  }

  /**
   * Concurrent version of removeFirstFiveTest.Add 10 identifier, remove first five.
   * Check the correct ones removed.
   */
  @Test
  void concurrentRemoveFirstFiveTest() throws IOException {
    int concurrencyDegree = 10;
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch doneAdd = new CountDownLatch(concurrencyDegree);
    Thread[] addThreads = new Thread[concurrencyDegree];

    /*
    Adding all concurrently.
     */
    for (int i = 0; i < allIds.size(); i++) {
      int finalI = i;
      addThreads[i] = new Thread(() -> {
        if (!db.add(allIds.get(finalI))) {
          threadError.getAndIncrement();
        }
        doneAdd.countDown();
      });
    }

    for (Thread t : addThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = doneAdd.await(60, TimeUnit.SECONDS);
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
        if (!db.remove(allIds.get(finalI))) {
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

    /*
     Checking for Has concurrently.
     */
    CountDownLatch doneHas = new CountDownLatch(concurrencyDegree);
    Thread[] hasThreads = new Thread[concurrencyDegree];
    for (int i = 0; i < allIds.size(); i++) {
      int finalI = i;
      int finalI1 = i;
      hasThreads[i] = new Thread(() -> {
        if (allIds.indexOf(allIds.get(finalI)) < 5) {
          if (db.has(allIds.get(finalI1))) {
            threadError.getAndIncrement();
          }
        } else {
          if (!db.has(allIds.get(finalI))) {
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

    Assertions.assertEquals(0, threadError.get());
    db.closeDb();
    FileUtils.deleteDirectory(new File(tempdir.toString()));
  }

  /**
   * Add 10 new identifiers and check that all of them are added correctly, i.e., while adding each identifier.
   * Add must return true.
   * Has returns true for each of them, and All returns list of all of them.
   * Then try Adding all of them again, and Add should return false for each of them.
   */
  @Test
  void duplicationTest() throws IOException {
    for (Identifier identifier : allIds) {
      Assertions.assertTrue(db.add(identifier));
    }
    for (Identifier identifier : allIds) {
      Assertions.assertTrue(db.has(identifier));
    }
    for (Identifier identifier : db.all()) {
      Assertions.assertTrue(allIds.contains(identifier));
    }
    for (Identifier identifier : allIds) {
      Assertions.assertFalse(db.add(identifier));
    }
    db.closeDb();
    FileUtils.deleteDirectory(new File(tempdir.toString()));
  }

  /**
   * Concurrent version of duplicationTest.
   */
  @Test void concurrentDuplicationTest() throws IOException {
    int concurrencyDegree = 10;

    /*
     * Adding all concurrently.
     */
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch doneAdd = new CountDownLatch(concurrencyDegree);
    Thread[] addThreads = new Thread[concurrencyDegree];
    for (int i = 0; i < allIds.size(); i++) {
      int finalI = i;
      addThreads[i] = new Thread(() -> {
        if (!db.add(allIds.get(finalI))) {
          threadError.getAndIncrement();
        }
        doneAdd.countDown();
      });
    }

    for (Thread t : addThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = doneAdd.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }

    CountDownLatch doneHas = new CountDownLatch(concurrencyDegree);
    Thread[] allThreads = new Thread[concurrencyDegree];
    for (int i = 0; i < allIds.size(); i++) {
      int finalI = i;
      allThreads[i] = new Thread(() -> {
        if (!db.has(allIds.get(finalI))) {
          threadError.getAndIncrement();
        }
        doneHas.countDown();
      });
    }
    for (Thread t : allThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = doneHas.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }

    CountDownLatch doneDubAdd = new CountDownLatch(concurrencyDegree);
    Thread[] addThreadsDup = new Thread[concurrencyDegree];
    for (int i = 0; i < allIds.size(); i++) {
      int finalI = i;
      addThreadsDup[i] = new Thread(() -> {
        if (db.add(allIds.get(finalI))) {
          threadError.getAndIncrement();
        }
        doneDubAdd.countDown();
      });
    }

    for (Thread t : addThreadsDup) {
      t.start();
    }
    try {
      boolean doneOneTime = doneDubAdd.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }

    Assertions.assertEquals(0, threadError.get());
    db.closeDb();
    FileUtils.deleteDirectory(new File(tempdir.toString()));
  }
}
