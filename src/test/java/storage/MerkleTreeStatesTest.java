package storage;

import model.lightchain.Identifier;
import modules.ads.merkletree.MerkleTreeState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import storage.mapdb.IdentifierMapDb;
import storage.mapdb.MerkleTreeStateMapDb;
import unittest.fixtures.IdentifierFixture;
import unittest.fixtures.MerkleTreeStateFixture;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Encapsulates tests for merkle tree states database.
 */
public class MerkleTreeStatesTest {
  private static final String TEMP_DIR = "tempdir";
  private static final String TEMP_FILE = "tempfile.db";
  private Path tempdir;
  private ArrayList<MerkleTreeState> allStates;
  private MerkleTreeStateMapDb db;

  /**
   * Set the tests up.
   */
  @BeforeEach
  void setUp() throws IOException {
    Path currentRelativePath = Paths.get("");
    tempdir = Files.createTempDirectory(currentRelativePath, TEMP_DIR);
    db = new MerkleTreeStateMapDb(tempdir.toAbsolutePath() + "/" + TEMP_FILE);
    allStates = MerkleTreeStateFixture.newStates(10);
  }

  /**
   * When adding 10 new merkle tree states sequentially, the Add method must return true for all of them.
   */
  @Test
  void sequentialAddTest() throws IOException {
    for (MerkleTreeState state : allStates) {
      Assertions.assertTrue(db.add(state));
    }
    for (MerkleTreeState state : allStates) {
      Assertions.assertTrue(db.has(state));
    }

    ArrayList<MerkleTreeState> all = db.all();
    Assertions.assertEquals(all.size(), 10);
    for (MerkleTreeState state : all) {
      Assertions.assertTrue(allStates.contains(state));
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
    Adding all states concurrently.
     */
    for (int i = 0; i < allStates.size(); i++) {
      int finalI = i;
      addThreads[i] = new Thread(() -> {
        if (!db.add(allStates.get(finalI))) {
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
    for (int i = 0; i < allStates.size(); i++) {
      int finalI = i;
      hasThreads[i] = new Thread(() -> {
        if (!db.has(allStates.get(finalI))) {
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
    ArrayList<MerkleTreeState> all = db.all();

    for (int i = 0; i < all.size(); i++) {
      int finalI = i;
      allThreads[i] = new Thread(() -> {
        if (!allStates.contains(allStates.get(finalI))) {
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
   * Add 10 new states, check that they are added correctly, i.e., while adding each state Add must return.
   * true, Has returns true for each of them, and All returns list of all of them. Then Remove the first 5 states.
   * While Removing each of them, the Remove should return true. Then query all 10 states using Has.
   * Has should return false for the first 5 states that have been removed. But for the last 5 states it.
   * should return true. Also, All should return only the last 5 states.
   */
  @Test
  void removeFirstFiveTest() throws IOException {

    for (MerkleTreeState state : allStates) {
      Assertions.assertTrue(db.add(state));
    }
    // removes the first five
    for (int i = 0; i < 5; i++) {
      Assertions.assertTrue(db.remove(allStates.get(i)));
    }
    for (int i = 0; i < 10; i++) {
      if (i < 5) {
        Assertions.assertFalse(db.has(allStates.get(i)) || db.all().contains(allStates.get(i)));
      } else {
        Assertions.assertTrue(db.has(allStates.get(i)) && db.all().contains(allStates.get(i)));
      }
    }
    db.closeDb();
    FileUtils.deleteDirectory(new File(tempdir.toString()));
  }

  /**
   * Concurrent version of removeFirstFiveTest.Add 10 states, remove first five.
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
    for (int i = 0; i < allStates.size(); i++) {
      int finalI = i;
      addThreads[i] = new Thread(() -> {
        if (!db.add(allStates.get(finalI))) {
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
        if (!db.remove(allStates.get(finalI))) {
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
    for (int i = 0; i < allStates.size(); i++) {
      int finalI = i;
      int finalI1 = i;
      hasThreads[i] = new Thread(() -> {
        if (allStates.indexOf(allStates.get(finalI)) < 5) {
          if (db.has(allStates.get(finalI1))) {
            threadError.getAndIncrement();
          }
        } else {
          if (!db.has(allStates.get(finalI))) {
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
   * Add 10 new states and check that all of them are added correctly, i.e., while adding each state.
   * Add must return true.
   * Has returns true for each of them, and All returns list of all of them.
   * Then try Adding all of them again, and Add should return false for each of them.
   */
  @Test
  void duplicationTest() throws IOException {
    for (MerkleTreeState state : allStates) {
      Assertions.assertTrue(db.add(state));
    }
    for (MerkleTreeState state : allStates) {
      Assertions.assertTrue(db.has(state));
    }
    for (MerkleTreeState state : db.all()) {
      Assertions.assertTrue(allStates.contains(state));
    }
    for (MerkleTreeState state : allStates) {
      Assertions.assertFalse(db.add(state));
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
    for (int i = 0; i < allStates.size(); i++) {
      int finalI = i;
      addThreads[i] = new Thread(() -> {
        if (!db.add(allStates.get(finalI))) {
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
    for (int i = 0; i < allStates.size(); i++) {
      int finalI = i;
      allThreads[i] = new Thread(() -> {
        if (!db.has(allStates.get(finalI))) {
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
    for (int i = 0; i < allStates.size(); i++) {
      int finalI = i;
      addThreadsDup[i] = new Thread(() -> {
        if (db.add(allStates.get(finalI))) {
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
