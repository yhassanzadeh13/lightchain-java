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

import model.lightchain.Block;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import storage.mapdb.BlocksMapDb;
import unittest.fixtures.BlockFixture;

/**
 * Encapsulates tests for block database.
 */
public class BlocksTest {

  private static final String TEMP_DIR = "tempdir";
  private static final String TEMP_FILE_ID = "tempfileID.db";
  private static final String TEMP_FILE_HEIGHT = "tempfileHEIGHT.db";
  private Path tempdir;
  private ArrayList<Block> allBlocks;
  private BlocksMapDb db;
  // TODO: implement a unit test for each of the following scenarios:
  // IMPORTANT NOTE: each test must have a separate instance of database, and the database MUST only created on a
  // temporary directory.
  // In following tests by a "new" block, we mean a block that already does not exist in the database,
  // and by a "duplicate" block, we mean one that already exists in the database.
  // 1. When adding 10 new blocks sequentially, the Add method must return true for all of them. Moreover, after
  //    adding blocks is done, querying the Has method for each of the block should return true. After adding all blocks
  //    are done, each block must be retrievable using both its id (byId) as well as its height (byHeight). Also, when
  //    querying All method, list of all 10 block must be returned.
  // 2. Repeat test case 1 for concurrently adding blocks as well as concurrently querying the database for has, byId,
  //    and byHeight.
  // 3. Add 10 new blocks sequentially, check that they are added correctly, i.e., while adding each block
  //    Add must return
  //    true, Has returns true for each of them, each block is retrievable by both its height and its identifier,
  //    and All returns list of all of them. Then Remove the first 5 blocks sequentially.
  //    While Removing each of them, the Remove should return true. Then query all 10 blocks using has, byId,
  //    and byHeight.
  //    Has should return false for the first 5 blocks have been removed,
  //    and byId and byHeight should return null. But for the last 5 blocks, has should return true, and byId
  //    and byHeight should successfully retrieve the exact block. Also, All should return only the last 5 blocks.
  // 4. Repeat test case 3 for concurrently adding and removing blocks as well as concurrently querying the
  //    database for has, byId, and byHeight.
  // 5. Add 10 new blocks and check that all of them are added correctly, i.e., while adding each block
  //    Add must return true, has returns true for each of them, and All returns list of all of them. Moreover, each
  //    block is retrievable using its identifier (byId) and height (byHeight). Then try Adding all of them again, and
  //    Add should return false for each of them, while has should still return true, and byId and byHeight should be
  //    able to retrieve the block.
  // 6. Repeat test case 5 for concurrently adding blocks as well as concurrently querying the
  //    database for has, byId, and byHeight.

  /**
   * Set the tests up.
   */
  @BeforeEach
  void setUp() throws IOException {
    Path currentRelativePath = Paths.get("");
    tempdir = Files.createTempDirectory(currentRelativePath, TEMP_DIR);
    db = new BlocksMapDb(tempdir.toAbsolutePath() + "/" + TEMP_FILE_ID,
        tempdir.toAbsolutePath() + "/" + TEMP_FILE_HEIGHT);
    allBlocks = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      allBlocks.add(BlockFixture.newBlock());
    }
  }

  /**
   * Adding blocks sequentially.
   *
   * @throws IOException throw IOException.
   */
  @Test
  void sequentialAddTest() throws IOException {
    for (Block block : allBlocks) {
      Assertions.assertTrue(db.add(block));
    }
    for (Block block : allBlocks) {
      Assertions.assertTrue(db.has(block.id()));
    }
    for (Block block : allBlocks) {
      Assertions.assertTrue(allBlocks.contains(db.atHeight(block.getHeight())));
    }
    for (Block block : allBlocks) {
      Assertions.assertTrue(allBlocks.contains(db.byId(block.id())));
    }
    ArrayList<Block> all = db.all();
    Assertions.assertEquals(all.size(), 10);
    for (Block block : all) {
      Assertions.assertTrue(allBlocks.contains(block));
    }
    db.closeDb();
    FileUtils.deleteDirectory(new File(tempdir.toString()));
  }

  /**
   * Adding blocks concurrently.
   */
  @Test
  void concurrentAddTest() throws IOException {
    int concurrencyDegree = 10;

    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch addDone = new CountDownLatch(concurrencyDegree);
    Thread[] addThreads = new Thread[concurrencyDegree];
    /*
    Adding all blocks concurrently.
    */
    for (int i = 0; i < allBlocks.size(); i++) {
      int finalI = i;
      addThreads[i] = new Thread(() -> {
        if (!db.add(allBlocks.get(finalI))) {
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
    for (int i = 0; i < allBlocks.size(); i++) {
      int finalI = i;
      hasThreads[i] = new Thread(() -> {
        if (!db.has((allBlocks.get(finalI)).id())) {
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
    Checking correctness of insertion byID.
    */
    CountDownLatch getDone = new CountDownLatch(concurrencyDegree);
    Thread[] getThreads = new Thread[concurrencyDegree];
    for (int i = 0; i < allBlocks.size(); i++) {
      int finalI = i;
      getThreads[i] = new Thread(() -> {
        if (!allBlocks.contains(db.byId(allBlocks.get(finalI).id()))) {
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
    Checking correctness of insertion by atHeight.
    */
    CountDownLatch heightDone = new CountDownLatch(concurrencyDegree);
    Thread[] heightThreats = new Thread[concurrencyDegree];
    for (int i = 0; i < allBlocks.size(); i++) {
      int finalI = i;
      heightThreats[i] = new Thread(() -> {
        if (!allBlocks.contains(db.atHeight(allBlocks.get(finalI).getHeight()))) {
          threadError.getAndIncrement();
        }
        heightDone.countDown();
      });
    }

    for (Thread t : heightThreats) {
      t.start();
    }
    try {
      boolean doneOneTime = heightDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    /*
    Retrieving all concurrently.
    */
    CountDownLatch doneAll = new CountDownLatch(concurrencyDegree);
    Thread[] allThreads = new Thread[concurrencyDegree];
    ArrayList<Block> all = db.all();
    for (int i = 0; i < all.size(); i++) {
      int finalI = i;
      allThreads[i] = new Thread(() -> {
        if (!all.contains(allBlocks.get(finalI))) {
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
   * Add 10 new blocks, remove first 5 and test methods.
   */
  @Test
  void removeFirstFiveTest() throws IOException {
    for (Block block : allBlocks) {
      Assertions.assertTrue(db.add(block));
    }
    for (Block block : allBlocks) {
      Assertions.assertTrue(db.has(block.id()));
    }
    for (Block block : allBlocks) {
      Assertions.assertTrue(allBlocks.contains(db.atHeight(block.getHeight())));
    }
    for (Block block : allBlocks) {
      Assertions.assertTrue(allBlocks.contains(db.byId(block.id())));
    }
    ArrayList<Block> all = db.all();
    Assertions.assertEquals(all.size(), 10);
    for (Block block : all) {
      Assertions.assertTrue(allBlocks.contains(block));
    }
    for (int i = 0; i < 5; i++) {
      Assertions.assertTrue(db.remove(allBlocks.get(i).id()));
    }
    for (int i = 0; i < 10; i++) {
      if (i < 5) {
        Assertions.assertFalse(db.has(allBlocks.get(i).id()) || db.all().contains(allBlocks.get(i)));
      } else {
        Assertions.assertTrue(db.has(allBlocks.get(i).id()) && db.all().contains(allBlocks.get(i))
            && db.all().contains(db.atHeight(allBlocks.get(i).getHeight()))
            && db.all().contains(db.byId(allBlocks.get(i).id())));
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
    Adding all blocks concurrently.
    */
    for (int i = 0; i < allBlocks.size(); i++) {
      int finalI = i;
      addThreads[i] = new Thread(() -> {
        if (!db.add(allBlocks.get(finalI))) {
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
    for (int i = 0; i < allBlocks.size(); i++) {
      int finalI = i;
      hasThreads[i] = new Thread(() -> {
        if (!db.has((allBlocks.get(finalI)).id())) {
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
    Checking correctness of insertion byID.
    */
    CountDownLatch getDone = new CountDownLatch(concurrencyDegree);
    Thread[] getThreads = new Thread[concurrencyDegree];
    for (int i = 0; i < allBlocks.size(); i++) {
      int finalI = i;
      getThreads[i] = new Thread(() -> {
        if (!allBlocks.contains(db.byId(allBlocks.get(finalI).id()))) {
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
    Checking correctness of insertion by atHeight.
    */
    CountDownLatch heightDone = new CountDownLatch(concurrencyDegree);
    Thread[] heightThreats = new Thread[concurrencyDegree];
    for (int i = 0; i < allBlocks.size(); i++) {
      int finalI = i;
      heightThreats[i] = new Thread(() -> {
        if (!allBlocks.contains(db.atHeight(allBlocks.get(finalI).getHeight()))) {
          threadError.getAndIncrement();
        }
        heightDone.countDown();
      });
    }

    for (Thread t : heightThreats) {
      t.start();
    }
    try {
      boolean doneOneTime = heightDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    /*
    Retrieving all concurrently.
    */
    CountDownLatch doneAll = new CountDownLatch(concurrencyDegree);
    Thread[] allThreads = new Thread[concurrencyDegree];
    ArrayList<Block> all = db.all();
    for (int i = 0; i < all.size(); i++) {
      int finalI = i;
      allThreads[i] = new Thread(() -> {
        if (!all.contains(allBlocks.get(finalI))) {
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
    Removing first 5 concurrently
     */
    int removeTill = concurrencyDegree / 2;
    CountDownLatch doneRemove = new CountDownLatch(removeTill);
    Thread[] removeThreads = new Thread[removeTill];
    for (int i = 0; i < removeTill; i++) {
      int finalI = i;
      removeThreads[i] = new Thread(() -> {
        if (!db.remove(allBlocks.get(finalI).id())) {
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
    Check Has after removing first five blocks
     */
    CountDownLatch doneHas = new CountDownLatch(concurrencyDegree / 2);
    Thread[] hasThreads2 = new Thread[concurrencyDegree / 2];
    for (int i = 0; i < concurrencyDegree / 2; i++) {
      int finalI = i;
      hasThreads2[i] = new Thread(() -> {
        if (allBlocks.indexOf(allBlocks.get(finalI)) < 5) {
          if (db.has(allBlocks.get(finalI).id())) {
            threadError.getAndIncrement();
          }
        } else {
          if (!db.has(allBlocks.get(finalI).id())) {
            threadError.getAndIncrement();
          }
        }
        doneHas.countDown();
      });
    }
    for (Thread t : hasThreads2) {
      t.start();
    }
    try {
      boolean doneOneTime = doneHas.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    /*
    Check byID after removing first five blocks
     */
    CountDownLatch getById = new CountDownLatch(concurrencyDegree / 2);
    Thread[] getThreadsById = new Thread[concurrencyDegree / 2];
    for (int i = 0; i < concurrencyDegree / 2; i++) {
      int finalI = i;
      int finalI1 = i + 5;
      getThreadsById[i] = new Thread(() -> {
        if (allBlocks.contains(db.byId(allBlocks.get(finalI).id()))
            || !allBlocks.contains(db.byId(allBlocks.get(finalI1).id()))) {
          System.out.println("here");
          threadError.getAndIncrement();
        }
        getById.countDown();
      });
    }

    for (Thread t : getThreadsById) {
      t.start();
    }
    try {
      boolean doneOneTime = getById.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    /*
    Check atHeight after removing first five blocks
     */
    CountDownLatch getByHeight = new CountDownLatch(concurrencyDegree / 2);
    Thread[] getThreadsByHeight = new Thread[concurrencyDegree / 2];
    for (int i = 0; i < concurrencyDegree / 2; i++) {
      int finalI = i;
      int finalI1 = i + 5;
      getThreadsByHeight[i] = new Thread(() -> {
        if (allBlocks.contains(db.atHeight(allBlocks.get(finalI).getHeight()))
            || !allBlocks.contains(db.atHeight(allBlocks.get(finalI1).getHeight()))) {

          threadError.getAndIncrement();
        }
        getByHeight.countDown();
      });
    }
    for (Thread t : getThreadsByHeight) {
      t.start();
    }
    try {
      boolean doneOneTime = getByHeight.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    Assertions.assertEquals(0, threadError.get());
    db.closeDb();
    FileUtils.deleteDirectory(new File(tempdir.toString()));
  }

  /**
   * Add 10 blocks already exist and return false expected.
   */
  @Test
  void duplicationTest() throws IOException {
    for (Block block : allBlocks) {
      Assertions.assertTrue(db.add(block));
    }
    for (Block block : allBlocks) {
      Assertions.assertTrue(db.has(block.id()));
    }

    for (Block block : allBlocks) {
      Assertions.assertTrue(allBlocks.contains(db.atHeight(block.getHeight())));
    }
    for (Block block : allBlocks) {
      Assertions.assertTrue(allBlocks.contains(db.byId(block.id())));
    }
    ArrayList<Block> all = db.all();
    Assertions.assertEquals(all.size(), 10);
    for (Block block : all) {
      Assertions.assertTrue(allBlocks.contains(block));
    }
    for (Block block : allBlocks) {
      Assertions.assertFalse(db.add(block));
    }
    /*
   After trying duplication, check again.
    */
    for (Block block : allBlocks) {
      Assertions.assertTrue(db.has(block.id()));
    }

    for (Block block : allBlocks) {
      Assertions.assertTrue(allBlocks.contains(db.atHeight(block.getHeight())));
    }
    for (Block block : allBlocks) {
      Assertions.assertTrue(allBlocks.contains(db.byId(block.id())));
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
    Adding all blocks concurrently.
     */
    for (int i = 0; i < allBlocks.size(); i++) {
      int finalI = i;
      addThreads[i] = new Thread(() -> {
        if (!db.add(allBlocks.get(finalI))) {
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
    for (int i = 0; i < allBlocks.size(); i++) {
      int finalI = i;
      hasThreads[i] = new Thread(() -> {
        if (!db.has((allBlocks.get(finalI)).id())) {
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
    Checking correctness of insertion byID.
     */
    CountDownLatch getDone = new CountDownLatch(concurrencyDegree);
    Thread[] getThreads = new Thread[concurrencyDegree];
    for (int i = 0; i < allBlocks.size(); i++) {
      int finalI = i;
      getThreads[i] = new Thread(() -> {
        if (!allBlocks.contains(db.byId(allBlocks.get(finalI).id()))) {
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
    Checking correctness of insertion by atHeight.
     */
    CountDownLatch heightDone = new CountDownLatch(concurrencyDegree);
    Thread[] heightThreats = new Thread[concurrencyDegree];
    for (int i = 0; i < allBlocks.size(); i++) {
      int finalI = i;
      heightThreats[i] = new Thread(() -> {
        if (!allBlocks.contains(db.atHeight(allBlocks.get(finalI).getHeight()))) {
          threadError.getAndIncrement();
        }
        heightDone.countDown();
      });
    }

    for (Thread t : heightThreats) {
      t.start();
    }
    try {
      boolean doneOneTime = heightDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    /*
    Retrieving all concurrently.
    */
    CountDownLatch doneAll = new CountDownLatch(concurrencyDegree);
    Thread[] allThreads = new Thread[concurrencyDegree];
    ArrayList<Block> all = db.all();
    for (int i = 0; i < all.size(); i++) {
      int finalI = i;
      allThreads[i] = new Thread(() -> {
        if (!all.contains(allBlocks.get(finalI))) {
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
    Adding existing blocks
     */
    CountDownLatch addDuplicateDone = new CountDownLatch(concurrencyDegree);
    Thread[] addDuplicateThreads = new Thread[concurrencyDegree];
    /*
    Adding all blocks concurrently.
     */
    for (int i = 0; i < allBlocks.size(); i++) {
      int finalI = i;
      addDuplicateThreads[i] = new Thread(() -> {
        if (db.add(allBlocks.get(finalI))) {
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
    Checking correctness of insertion by Has after duplication.
     */
    CountDownLatch hasDone2 = new CountDownLatch(concurrencyDegree);
    Thread[] hasThreads2 = new Thread[concurrencyDegree];
    for (int i = 0; i < allBlocks.size(); i++) {
      int finalI = i;
      hasThreads2[i] = new Thread(() -> {
        if (!db.has((allBlocks.get(finalI)).id())) {
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
    Checking correctness of insertion byID after duplication.
     */
    CountDownLatch getDone2 = new CountDownLatch(concurrencyDegree);
    Thread[] getThreads2 = new Thread[concurrencyDegree];
    for (int i = 0; i < allBlocks.size(); i++) {
      int finalI = i;
      getThreads2[i] = new Thread(() -> {
        if (!allBlocks.contains(db.byId(allBlocks.get(finalI).id()))) {
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
    /*
    Checking correctness of insertion by atHeight after duplication.
     */
    CountDownLatch heightDone2 = new CountDownLatch(concurrencyDegree);
    Thread[] heightThreats2 = new Thread[concurrencyDegree];
    for (int i = 0; i < allBlocks.size(); i++) {
      int finalI = i;
      heightThreats2[i] = new Thread(() -> {
        if (!allBlocks.contains(db.atHeight(allBlocks.get(finalI).getHeight()))) {
          threadError.getAndIncrement();
        }
        heightDone2.countDown();
      });
    }

    for (Thread t : heightThreats2) {
      t.start();
    }
    try {
      boolean doneOneTime = heightDone2.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    /*
    Retrieving all concurrently after duplication.
     */
    CountDownLatch doneAll2 = new CountDownLatch(concurrencyDegree);
    Thread[] allThreads2 = new Thread[concurrencyDegree];
    ArrayList<Block> all2 = db.all();
    for (int i = 0; i < all2.size(); i++) {
      int finalI = i;
      allThreads2[i] = new Thread(() -> {
        if (!all2.contains(allBlocks.get(finalI))) {
          threadError.getAndIncrement();
        }
        doneAll2.countDown();
      });
    }

    for (Thread t : allThreads2) {
      t.start();
    }
    try {
      boolean doneOneTime = doneAll2.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    Assertions.assertEquals(0, threadError.get());
    db.closeDb();
    FileUtils.deleteDirectory(new File(tempdir.toString()));
  }
}
