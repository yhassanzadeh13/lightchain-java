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

import model.Entity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import storage.mapdb.DistributedMapDb;
import unittest.fixtures.BlockFixture;
import unittest.fixtures.TransactionFixture;

/**
 * Encapsulates tests for distributed storage.
 */
public class DistributedMapDbTest {

  private static final String TEMP_DIR = "tempdir";
  private static final String TEMP_FILE_ID = "tempfileID.db";
  private Path tempdir;
  private ArrayList<Entity> allEntities;
  private DistributedMapDb db;
  // TODO: implement a unit test for each of the following scenarios:
  // IMPORTANT NOTE: each test must have a separate instance of database, and the database MUST only created on a
  // temporary directory.
  // In following tests by a "new" entity, we mean an entity that already does not exist in the database,
  // and by a "duplicate" entity, we mean one that already exists in the database.
  // 1. When adding 20 new entities of different types (10 transactions and 10 blocks) sequentially,
  //    the Add method must return true for all of them. Moreover, after
  //    adding entities are done, querying the Has method for each of the entities should return true.
  //    After adding all entities
  //    are done, each entity must be retrievable using both its id (get). Also, when
  //    querying All method, list of all 20 entities must be returned.
  // 2. Repeat test case 1 for concurrently adding entities as well as concurrently querying the database for has, and
  //    get.
  // 3. Add 20 new entities sequentially (10 transactions and 10 blocks), check that they are added correctly, i.e.,
  //    while adding each entity Add must return
  //    true, Has returns true for each of them, each entity is retrievable by its identifier,
  //    and All returns list of all of them.
  //    Then Remove the first 10 entities (5 blocks and 5 transactions) sequentially.
  //    While Removing each of them, the Remove should return true. Then query all 20 entities using has, and get.
  //    Has should return false for the first 5 blocks amd 5 transactions that have been removed,
  //    and get should return null. But for the last 5 blocks and 5 transactions, has should return true, and get
  //    should successfully retrieve the exact entity.
  //    Also, All should return only the last 5 blocks and 5 transactions.
  // 4. Repeat test case 3 for concurrently adding and removing entities as well as concurrently querying the
  //    database for has, and get.
  // 5. Add 20 new entities (10 blocks, and 10 transactions)
  //    and check that all of them are added correctly, i.e., while adding each entity
  //    Add must return true, has returns true for each of them, and All returns list of all of them. Moreover, each
  //    entity is retrievable using its identifier (get). Then try Adding all of them again, and
  //    Add should return false for each of them, while has should still return true, and get should be
  //    able to retrieve the entity.
  // 6. Repeat test case 5 for concurrently adding entities as well as concurrently querying the
  //    database for has, get.

  /**
   * Set the tests up.
   */
  @BeforeEach
  void setUp() throws IOException {
    Path currentRelativePath = Paths.get("");
    tempdir = Files.createTempDirectory(currentRelativePath, TEMP_DIR);
    db = new DistributedMapDb(tempdir.toAbsolutePath() + "/" + TEMP_FILE_ID);
    allEntities = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      allEntities.add(BlockFixture.newBlock(10));
      allEntities.add(TransactionFixture.newTransaction(10));
    }
  }

  /**
   * Adding entities sequentially.
   *
   * @throws IOException throw IOException.
   */
  @Test
  void sequentialAddTest() throws IOException {
    for (Entity entity : allEntities) {
      Assertions.assertTrue(db.add(entity));
    }
    for (Entity entity : allEntities) {
      Assertions.assertTrue(db.has(entity.id()));
    }
    for (Entity entity : allEntities) {
      Assertions.assertTrue(allEntities.contains(db.get(entity.id())));
    }
    ArrayList<Entity> all = db.all();
    Assertions.assertEquals(all.size(), 20);
    for (Entity entity : allEntities) {
      Assertions.assertTrue(all.contains(entity));
    }
    db.closeDb();
    try {
      FileUtils.deleteDirectory(new File(tempdir.toString()));
    } catch (IOException e) {
      throw new IOException("could not delete directory");
    }
  }

  /**
   * Concurrent version of adding entities.
   */
  @Test
  void concurrentAddTest() throws IOException {
    int concurrencyDegree = 20;

    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch addDone = new CountDownLatch(concurrencyDegree);
    Thread[] addThreads = new Thread[concurrencyDegree];
    /*
    Adding all transactions concurrently.
     */
    for (int i = 0; i < allEntities.size(); i++) {
      int finalI = i;
      addThreads[i] = new Thread(() -> {
        if (!db.add(allEntities.get(finalI))) {
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
    for (int i = 0; i < allEntities.size(); i++) {
      int finalI = i;
      hasThreads[i] = new Thread(() -> {
        if (!db.has((allEntities.get(finalI)).id())) {
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
    for (int i = 0; i < allEntities.size(); i++) {
      int finalI = i;
      getThreads[i] = new Thread(() -> {
        if (!allEntities.contains(db.get(allEntities.get(finalI).id()))) {
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
    CountDownLatch doneAll = new CountDownLatch(concurrencyDegree);
    Thread[] allThreads = new Thread[concurrencyDegree];
    ArrayList<Entity> all = db.all();
    Assertions.assertEquals(all.size(), 20);
    for (int i = 0; i < all.size(); i++) {
      int finalI = i;
      allThreads[i] = new Thread(() -> {
        if (!all.contains(allEntities.get(finalI))) {
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
    try {
      FileUtils.deleteDirectory(new File(tempdir.toString()));
    } catch (IOException e) {
      throw new IOException("could not delete directory");
    }
  }

  /**
   * Remove the first 10 entities and test methods.
   *
   * @throws IOException for any unhappy path.
   */
  @Test
  void removeFirstTenTest() throws IOException {
    for (Entity entity : allEntities) {
      Assertions.assertTrue(db.add(entity));
    }
    for (int i = 0; i < 10; i++) {
      Assertions.assertTrue(db.remove(allEntities.get(i)));
    }
    for (int i = 0; i < 20; i++) {
      if (i < 10) {
        Assertions.assertFalse(db.has(allEntities.get(i).id()) || db.all().contains(allEntities.get(i)));
      } else {
        Assertions.assertTrue(db.has(allEntities.get(i).id()) && db.all().contains(allEntities.get(i)));
      }
    }
    db.closeDb();
    try {
      FileUtils.deleteDirectory(new File(tempdir.toString()));
    } catch (IOException e) {
      throw new IOException("could not delete directory");
    }
  }

  /**
   * Concurrent version of remove first ten test.
   */
  @Test
  void concurrentRemoveFirstTenTest() throws IOException {
    int concurrencyDegree = 20;

    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch addDone = new CountDownLatch(concurrencyDegree);
    Thread[] addThreads = new Thread[concurrencyDegree];
    /*
    Adding all transactions concurrently.
     */
    for (int i = 0; i < allEntities.size(); i++) {
      int finalI = i;
      addThreads[i] = new Thread(() -> {
        if (!db.add(allEntities.get(finalI))) {
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
    Removing first 10 concurrently
     */
    int removeTill = concurrencyDegree / 2;
    CountDownLatch doneRemove = new CountDownLatch(removeTill);
    Thread[] removeThreads = new Thread[removeTill];
    for (int i = 0; i < removeTill; i++) {
      int finalI = i;
      removeThreads[i] = new Thread(() -> {
        if (!db.remove(allEntities.get(finalI))) {
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
    Check Has method after removing.
     */
    CountDownLatch doneHas = new CountDownLatch(concurrencyDegree);
    Thread[] hasThreads = new Thread[concurrencyDegree];
    for (int i = 0; i < allEntities.size(); i++) {
      int finalI = i;
      int finalI1 = i;
      hasThreads[i] = new Thread(() -> {
        if (allEntities.indexOf(allEntities.get(finalI)) < 10) {
          if (db.has(allEntities.get(finalI1).id())) {
            threadError.getAndIncrement();
          }
        } else {
          if (!db.has(allEntities.get(finalI).id())) {
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
    /*
    Check get method after removing.
     */
    CountDownLatch getDone = new CountDownLatch(concurrencyDegree / 2);
    Thread[] getThreads = new Thread[concurrencyDegree / 2];
    for (int i = 0; i < concurrencyDegree / 2; i++) {
      int finalI = i;
      int finalI1 = i + 10;
      getThreads[i] = new Thread(() -> {
        if (allEntities.contains(db.get(allEntities.get(finalI).id()))
            || !allEntities.contains(db.get(allEntities.get(finalI1).id()))) {
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
    Assertions.assertEquals(0, threadError.get());
    db.closeDb();
    try {
      FileUtils.deleteDirectory(new File(tempdir.toString()));
    } catch (IOException e) {
      throw new IOException("could not delete directory");
    }
  }

  /**
   * Add 20 entities already exist and return false expected.
   *
   * @throws IOException for any unhappy path.
   */
  @Test
  void duplicationTest() throws IOException {
    for (Entity entity : allEntities) {
      Assertions.assertTrue(db.add(entity));
    }
    for (Entity entity : allEntities) {
      Assertions.assertTrue(db.has(entity.id()));
    }
    ArrayList<Entity> all = db.all();
    Assertions.assertEquals(all.size(), 20);
    for (Entity entity : all) {
      Assertions.assertTrue(allEntities.contains(entity));
    }
    for (Entity entity : allEntities) {
      Assertions.assertTrue(allEntities.contains(db.get(entity.id())));
    }
    for (Entity entity : allEntities) {
      Assertions.assertFalse(db.add(entity));
    }
    /*
    After trying duplication, check again.
    */
    for (Entity entity : allEntities) {
      Assertions.assertTrue(db.has(entity.id()));
    }
    for (Entity entity : allEntities) {
      Assertions.assertTrue(allEntities.contains(db.get(entity.id())));
    }
    db.closeDb();
    try {
      FileUtils.deleteDirectory(new File(tempdir.toString()));
    } catch (IOException e) {
      throw new IOException("could not delete directory");
    }
  }

  /**
   * Concurrent version of duplication test.
   *
   * @throws IOException for any unhappy path for dir deletion.
   */
  @Test
  void concurrentDuplicationTest() throws IOException {
    int concurrencyDegree = 20;

    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch addDone = new CountDownLatch(concurrencyDegree);
    Thread[] addThreads = new Thread[concurrencyDegree];
    /*
    Adding all transactions concurrently.
     */
    for (int i = 0; i < allEntities.size(); i++) {
      int finalI = i;
      addThreads[i] = new Thread(() -> {
        if (!db.add(allEntities.get(finalI))) {
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
    for (int i = 0; i < allEntities.size(); i++) {
      int finalI = i;
      hasThreads[i] = new Thread(() -> {
        if (!db.has((allEntities.get(finalI)).id())) {
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
    for (int i = 0; i < allEntities.size(); i++) {
      int finalI = i;
      getThreads[i] = new Thread(() -> {
        if (!allEntities.contains(db.get(allEntities.get(finalI).id()))) {
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
    ArrayList<Entity> all = db.all();
    Assertions.assertEquals(all.size(), 20);
    for (int i = 0; i < all.size(); i++) {
      int finalI = i;
      allThreads[i] = new Thread(() -> {
        if (!all.contains(allEntities.get(finalI))) {
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
    Adding existing entities.
     */
    CountDownLatch addDuplicateDone = new CountDownLatch(concurrencyDegree);
    Thread[] addDuplicateThreads = new Thread[concurrencyDegree];
    /*
    Adding all transactions concurrently.
     */
    for (int i = 0; i < allEntities.size(); i++) {
      int finalI = i;
      addDuplicateThreads[i] = new Thread(() -> {
        if (db.add(allEntities.get(finalI))) {
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
    for (int i = 0; i < allEntities.size(); i++) {
      int finalI = i;
      hasThreads2[i] = new Thread(() -> {
        if (!db.has((allEntities.get(finalI)).id())) {
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
    for (int i = 0; i < allEntities.size(); i++) {
      int finalI = i;
      getThreads2[i] = new Thread(() -> {
        if (!allEntities.contains(db.get(allEntities.get(finalI).id()))) {
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
    Assertions.assertEquals(0, threadError.get());
    db.closeDb();
    try {
      FileUtils.deleteDirectory(new File(tempdir.toString()));
    } catch (IOException e) {
      throw new IOException("could not delete directory");
    }
  }

}
