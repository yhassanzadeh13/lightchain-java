package storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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
  private ArrayList<Identifier> identifierArrayList;
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
    identifierArrayList = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      Identifier identifier = IdentifierFixture.newIdentifier();
      identifierArrayList.add(identifier);
    }
  }

  /**
   * When adding 10 new identifiers sequentially, the Add method must return true for all of them.
   */
  @Test
  void sequentialAddTest() throws IOException {
    for (Identifier identifier : identifierArrayList) {
      Assertions.assertTrue(db.add(identifier));
    }
    for (Identifier identifier : identifierArrayList) {
      Assertions.assertTrue(db.has(identifier));
    }
    // TODO: check correctness
    for (Identifier identifier : db.all()) {
      Assertions.assertTrue(identifierArrayList.contains(identifier));
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
    int count = 0;
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch countDownLatchAdd = new CountDownLatch(concurrencyDegree);
    Thread[] addThreads = new Thread[concurrencyDegree];
    for (Identifier identifier : identifierArrayList) {
      addThreads[count] = new Thread(() -> {
        if (!db.add(identifier)) {
          threadError.getAndIncrement();
        }
        countDownLatchAdd.countDown();
      });
      count++;
    }
    for (Thread t : addThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = countDownLatchAdd.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    count = 0;
    CountDownLatch countDownLatchHas = new CountDownLatch(concurrencyDegree);
    Thread[] hasThreads = new Thread[concurrencyDegree];
    for (Identifier identifier : identifierArrayList) {
      hasThreads[count] = new Thread(() -> {
        if (!db.has(identifier)) {
          threadError.getAndIncrement();
        }
        countDownLatchHas.countDown();
      });
      count++;
    }
    for (Thread t : hasThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = countDownLatchHas.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    count = 0;
    CountDownLatch countDownLatchAll = new CountDownLatch(concurrencyDegree);
    Thread[] allThreads = new Thread[concurrencyDegree];
    for (Identifier identifier : db.all()) {
      allThreads[count] = new Thread(() -> {
        if (!identifierArrayList.contains(identifier)) {
          threadError.getAndIncrement();
        }
        countDownLatchAll.countDown();
      });
      count++;
    }
    for (Thread t : allThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = countDownLatchAll.await(60, TimeUnit.SECONDS);
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

    for (Identifier identifier : identifierArrayList) {
      Assertions.assertTrue(db.add(identifier));
    }
    for (int x = 0; x < 5; x++) {
      Assertions.assertTrue(db.remove(identifierArrayList.get(x)));
    }
    for (int x = 0; x < 10; x++) {
      if (x < 5) {
        Assertions.assertFalse(db.has(identifierArrayList.get(x)) || db.all().contains(identifierArrayList.get(x)));
      } else {
        Assertions.assertTrue(db.has(identifierArrayList.get(x)) && db.all().contains(identifierArrayList.get(x)));
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
    int count = 0;
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch countDownLatchAdd = new CountDownLatch(concurrencyDegree);
    Thread[] addThreads = new Thread[concurrencyDegree];
    for (Identifier identifier : identifierArrayList) {
      addThreads[count] = new Thread(() -> {
        if (!db.add(identifier)) {
          threadError.getAndIncrement();
        }
        countDownLatchAdd.countDown();
      });
      count++;
    }
    for (Thread t : addThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = countDownLatchAdd.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    count = 0;
    CountDownLatch countDownLatchRemove = new CountDownLatch(concurrencyDegree / 2);
    Thread[] removeThreads = new Thread[concurrencyDegree / 2];
    for (Identifier identifier : identifierArrayList.subList(0,concurrencyDegree/2)) {
      removeThreads[count] = new Thread(() -> {
        if (!db.remove(identifier)) {
          threadError.getAndIncrement();
        }
        countDownLatchRemove.countDown();
      });
      count++;
    }
    for (Thread t : removeThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = countDownLatchRemove.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    count = 0;
    CountDownLatch countDownLatchHas = new CountDownLatch(concurrencyDegree);
    Thread[] hasThreads = new Thread[concurrencyDegree];
    for (Identifier identifier : identifierArrayList) {
      hasThreads[count] = new Thread(() -> {
        if (identifierArrayList.indexOf(identifier) < 5) {
          if (db.has(identifier)) {
            threadError.getAndIncrement();
          }
        } else {
          if (!db.has(identifier)) {
            threadError.getAndIncrement();
          }
        }
        countDownLatchHas.countDown();
      });
      count++;
    }
    for (Thread t : hasThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = countDownLatchHas.await(60, TimeUnit.SECONDS);
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
    for (Identifier identifier : identifierArrayList) {
      Assertions.assertTrue(db.add(identifier));
    }
    for (Identifier identifier : db.all()) {
      Assertions.assertTrue(identifierArrayList.contains(identifier));
    }
    for (Identifier identifier : identifierArrayList) {
      Assertions.assertFalse(db.add(identifier));
    }
    db.closeDb();
    FileUtils.deleteDirectory(new File(tempdir.toString()));
  }
}
