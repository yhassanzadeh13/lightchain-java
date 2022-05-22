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
import org.junit.jupiter.api.AfterEach;
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

  /**
   * Initialize database.
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
   * Closes database.
   *
   * @throws IOException if deleting temporary directory faces unhappy path.
   */
  @AfterEach
  void cleanup() throws IOException {
    db.closeDb();
    FileUtils.deleteDirectory(new File(tempdir.toString()));
  }


  /**
   * When adding 20 new entities of different types (10 transactions and 10 blocks) sequentially,
   * the Add method must return true for all of them. Moreover, after
   * adding entities are done, querying the Has method for each of the entities should return true.
   * After adding all entities
   * are done, each entity must be retrievable using both its id (get). Also, when
   * querying All method, list of all 20 entities must be returned.
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
   * When adding 20 new entities of different types (10 transactions and 10 blocks) CONCURRENTLY,
   * the Add method must return true for all of them. Moreover, after
   * adding entities are done, querying the Has method for each of the entities should return true.
   * After adding all entities
   * are done, each entity must be retrievable using both its id (get). Also, when
   * querying All method, list of all 20 entities must be returned.
   *
   */
  @Test
  void concurrentAddTest() {
    /*
     Adding all blocks concurrently.
     */
    this.addAllEntitiesConcurrently(true);

     /*
     All blocks should be retrievable
     */
    this.checkForHasConcurrently(0);
    this.checkForGetConcurrently(0);
    this.checkForAllConcurrently(0);

  }

  /**
   * Add 20 new entities sequentially (10 transactions and 10 blocks), check that they are added correctly, i.e.,
   * while adding each entity Add must return
   * true, Has returns true for each of them, each entity is retrievable by its identifier,
   * and All returns list of all of them.
   * Then Remove the first 10 entities (5 blocks and 5 transactions) sequentially.
   * While Removing each of them, the Remove should return true. Then query all 20 entities using has, and get.
   * Has should return false for the first 5 blocks amd 5 transactions that have been removed,
   * and get should return null. But for the last 5 blocks and 5 transactions, has should return true, and get
   * should successfully retrieve the exact entity.
   * Also, All should return only the last 5 blocks and 5 transactions.
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
   * Add 20 new entities CONCURRENTLY (10 transactions and 10 blocks), check that they are added correctly, i.e.,
   * while adding each entity Add must return
   * true, Has returns true for each of them, each entity is retrievable by its identifier,
   * and All returns list of all of them.
   * Then Remove the first 10 entities (5 blocks and 5 transactions) sequentially.
   * While Removing each of them, the Remove should return true. Then query all 20 entities using has, and get.
   * Has should return false for the first 5 blocks amd 5 transactions that have been removed,
   * and get should return null. But for the last 5 blocks and 5 transactions, has should return true, and get
   * should successfully retrieve the exact entity.
   * Also, All should return only the last 5 blocks and 5 transactions.
   *
   */
  @Test
  void concurrentRemoveFirstTenTest() {
 /*
     Adding all entities concurrently.
     */
    this.addAllEntitiesConcurrently(true);

     /*
     All entities should be retrievable.
     */
    this.checkForGetConcurrently(0);
    this.checkForHasConcurrently(0);
    this.checkForAllConcurrently(0);

     /*
     Removing first 10 concurrently
      */
    this.removeEntityTill(10);

     /*
     first five blocks must not be retrievable,
     the rest must be.
      */
    this.checkForGetConcurrently(10);
    this.checkForHasConcurrently(10);
    this.checkForAllConcurrently(10);
  }


  /**
   * Removes entities from blocks storage database till the given index concurrently.
   *
   * @param till exclusive index of the last entity being removed.
   */
  private void removeEntityTill(int till) {
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch doneRemove = new CountDownLatch(till);
    Thread[] removeThreads = new Thread[till];
    for (int i = 0; i < till; i++) {
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
    Assertions.assertEquals(0, threadError.get());

  }

  /**
   * Add 20 new entities sequentially (10 blocks, and 10 transactions)
   * and check that all of them are added correctly, i.e., while adding each entity
   * Add must return true, has returns true for each of them, and All returns list of all of them. Moreover, each
   * entity is retrievable using its identifier (get). Then try Adding all of them again, and
   * Add should return false for each of them, while has should still return true, and get should be
   * able to retrieve the entity.
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
   * Add 20 new entities concurrently (10 blocks, and 10 transactions)
   * and check that all of them are added correctly, i.e., while adding each entity
   * Add must return true, has returns true for each of them, and All returns list of all of them. Moreover, each
   * entity is retrievable using its identifier (get). Then try Adding all of them again, and
   * Add should return false for each of them, while has should still return true, and get should be
   * able to retrieve the entity.
   *
   */
  @Test
  void concurrentDuplicationTest() {
    /*
     Adding all entities concurrently.
      */
    this.addAllEntitiesConcurrently(true);
     /*
     All entities should be retrievable using their id or height.
     */
    this.checkForGetConcurrently(0);
    this.checkForHasConcurrently(0);
    this.checkForAllConcurrently(0);
     /*
     Adding all entities again concurrently, all should fail due to duplication.
      */
    this.addAllEntitiesConcurrently(false);
     /*
     Again, all entities should be retrievable using their id or height.
     */
    this.checkForGetConcurrently(0);
    this.checkForHasConcurrently(0);
    this.checkForAllConcurrently(0);
  }

  /**
   * Adds all entities to the distributed storage database till the given index concurrently.
   *
   * @param expectedResult expected boolean result after each insertion; true means entity added successfully,
   *                       false means entity was not added successfully.
   */
  private void addAllEntitiesConcurrently(boolean expectedResult) {
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch addDone = new CountDownLatch(allEntities.size());
    Thread[] addThreads = new Thread[allEntities.size()];
     /*
     Adding all blocks concurrently.
      */
    for (int i = 0; i < allEntities.size(); i++) {
      int finalI = i;
      addThreads[i] = new Thread(() -> {
        if (db.add(allEntities.get(finalI)) != expectedResult) {
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
   * Checks existence of entities in the entity storage database starting from the given index.
   *
   * @param from inclusive index of the first entity to check.
   */
  private void checkForHasConcurrently(int from) {
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch hasDone = new CountDownLatch(allEntities.size());
    Thread[] hasThreads = new Thread[allEntities.size()];
    for (int i = 0; i < allEntities.size(); i++) {
      int finalI = i;
      Entity entity = allEntities.get(i);

      hasThreads[i] = new Thread(() -> {
        if (finalI < from) {
          // blocks should not exist
          if (this.db.has(entity.id())) {
            threadError.incrementAndGet();
          }
        } else {
          // block should exist
          if (!this.db.has(entity.id())) {
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
   * Checks retrievability of entity from the distributed storage database starting from the given index.
   *
   * @param from inclusive index of the first entity to check.
   */
  private void checkForGetConcurrently(int from) {
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch getDone = new CountDownLatch(allEntities.size());
    Thread[] getThreads = new Thread[allEntities.size()];
    for (int i = 0; i < allEntities.size(); i++) {
      int finalI = i;
      Entity entity = allEntities.get(i);
      getThreads[i] = new Thread(() -> {
        Entity got = db.get(entity.id());
        if (finalI < from) {
          // blocks should not exist
          if (got != null) {
            threadError.incrementAndGet();
          }
        } else {
          // block should exist
          if (!entity.equals(got)) {
            threadError.getAndIncrement();
          }
          if (!entity.id().equals(got.id())) {
            threadError.getAndIncrement();
          }
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
  }

  /**
   * Checks retrievability of entities from the distributed storage database starting from the given index.
   *
   * @param from inclusive index of the first transaction to check.
   */
  private void checkForAllConcurrently(int from) {
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch doneAll = new CountDownLatch(allEntities.size());
    Thread[] allThreads = new Thread[allEntities.size()];
    ArrayList<Entity> all = db.all();
    for (int i = 0; i < allEntities.size(); i++) {
      int finalI = i;
      final Entity entity = allEntities.get(i);
      allThreads[i] = new Thread(() -> {
        if (finalI < from) {
          // blocks should not exist
          if (all.contains(entity)) {
            threadError.incrementAndGet();
          }
        } else {
          // block should exist
          if (!all.contains(entity)) {
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
}
