package storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import model.lightchain.BlockProposal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import storage.mapdb.BlockProposalsMapDb;
import unittest.fixtures.BlockFixture;

/**
 * This class encompasses all the tests for BlockProposals implementation.
 */
public class BlockProposalsTest {
  private static final String TEMP_DIR = "tempdir";
  private static final String TEMP_FILE = "tempfile.db";
  private static final int TEST_SIZE = 10;
  private Path tempdir;
  private ArrayList<BlockProposal> allBlockProposals;
  private BlockProposalsMapDb db;

  /**
   * Initializes database.
   *
   * @throws IOException if creating temporary directory faces unhappy path.
   */
  @BeforeEach
  void setup() throws IOException {
    Path currentRelativePath = Paths.get("");
    tempdir = Files.createTempDirectory(currentRelativePath, TEMP_DIR);
    db = new BlockProposalsMapDb(tempdir.toAbsolutePath() + "/" + TEMP_FILE);
    allBlockProposals = new ArrayList<>();
    for (int i = 0; i < TEST_SIZE; i++) {
      allBlockProposals.add(BlockFixture.newBlockProposal());
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
   * Test to set the last proposed block and match it against the
   * last proposed block in database.
   */
  @Test
  void setAndCheckLastBlockProposal() {
    for (BlockProposal blockProposal : allBlockProposals) {
      db.clearLastProposal();
      db.setLastProposal(blockProposal);
      Assertions.assertEquals(blockProposal.id(), db.getLastProposal().id());
    }
  }

  /**
   * Test to check the method setLastProposal throws
   * IllegalStateException when called with
   * last block proposal already set.
   */
  @Test
  void setExistingLastBlockProposal() {
    db.setLastProposal(allBlockProposals.get(0));
    for (BlockProposal blockProposal : allBlockProposals) {
      Throwable exception = Assertions.assertThrows(IllegalStateException.class,
          () -> db.setLastProposal(blockProposal));
      Assertions.assertEquals(
          BlockProposalsMapDb.LAST_BLOCK_PROPOSAL_EXISTS, exception.getMessage());
    }
  }

  /**
   * If multiple threads concurrently set different proposals as last proposal,
   * only one of them will succeed and rest will throw IllegalStateException
   * Lastly the set proposal must be one of them.
   */
  @Test
  void concurrentlySetLastBlockProposal() {
    ArrayList<Thread> threads = new ArrayList<Thread>();
    final CountDownLatch lastProposalSet = new CountDownLatch(allBlockProposals.size());

    for (BlockProposal blockProposal : allBlockProposals) {
      threads.add(new Thread() {
        @Override
        public void run() {
          try {
            db.setLastProposal(blockProposal);
            Assertions.assertEquals(db.getLastProposal().id(), blockProposal.id());
          } catch (Exception e) {
            Assertions.assertEquals(e.getMessage(), BlockProposalsMapDb.LAST_BLOCK_PROPOSAL_EXISTS);
          }
          lastProposalSet.countDown();
        }
      });
    }

    for (int i = 0; i < allBlockProposals.size(); i++) {
      try {
        threads.get(i).start();
      } catch (Exception e) {
        Assertions.fail(e);
      }
    }

    try {
      boolean doneOnTime = lastProposalSet.await(1, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOnTime);
    } catch (InterruptedException e) {
      Assertions.fail(e);
    }

    Assertions.assertTrue(allBlockProposals.contains(db.getLastProposal()));
  }

  /**
   * Test that the state of last proposal should not be distorted.
   * when BlockProposalsMapDb has the last proposal set,
   * 10 threads concurrently setting the last proposal,
   * thrown IllegalStateException.
   * Once all threads are done the last proposal must be the one
   * that initially was set on the database.
   */
  @Test
  void concurrentlySetExistingLastBlockProposal() {
    ArrayList<Thread> threads = new ArrayList<Thread>();
    final CountDownLatch lastProposalSet = new CountDownLatch(allBlockProposals.size());
    BlockProposal newBlockProposal = BlockFixture.newBlockProposal();
    db.clearLastProposal();
    db.setLastProposal(newBlockProposal);

    for (BlockProposal blockProposal : allBlockProposals) {
      threads.add(new Thread() {
        @Override
        public void run() {
          try {
            db.setLastProposal(blockProposal);
          } catch (Exception e) {
            Assertions.assertEquals(e.getMessage(), BlockProposalsMapDb.LAST_BLOCK_PROPOSAL_EXISTS);
          }
          lastProposalSet.countDown();
        }
      });
    }

    for (int i = 0; i < allBlockProposals.size(); i++) {
      try {
        threads.get(i).start();
      } catch (Exception e) {
        Assertions.fail(e);
      }
    }

    try {
      boolean doneOnTime = lastProposalSet.await(1, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOnTime);
    } catch (InterruptedException e) {
      Assertions.fail(e);
    }

    Assertions.assertEquals(db.getLastProposal(), newBlockProposal);
  }

  /**
   * When clearLastProposal is called on an empty database,
   * it does not have any effect, i.e., does not throw any exception.
   */
  @Test
  void clearLastProposalOnEmptyDatabase() {
    Assertions.assertDoesNotThrow(() -> db.clearLastProposal());
  }

  /**
   * When clearLastProposal is called, it clears the last set proposal,
   * hence getting the last proposal returns null.
   */
  @Test
  void clearAndGetLastProposal() {
    BlockProposal blockProposal = BlockFixture.newBlockProposal();
    db.setLastProposal(blockProposal);
    db.clearLastProposal();
    Assertions.assertNull(db.getLastProposal());
  }

  /**
   * When BlockProposalsMapDb has the last proposal set,
   * calling clearLastProposal multiple times sequentially
   * does not have any side effects except it only cleans
   * the last proposal (once), and the rest attempts are idempotent.
   */
  @Test
  void repeatedlyClearLastProposal() {
    BlockProposal blockProposal = allBlockProposals.get(0);

    db.setLastProposal(blockProposal);
    Assertions.assertEquals(db.getLastProposal(), blockProposal);

    db.clearLastProposal();
    Assertions.assertNull(db.getLastProposal());

    for (int i = 0; i < allBlockProposals.size(); i++) {
      Assertions.assertDoesNotThrow(() -> db.clearLastProposal());
    }

    Assertions.assertNull(db.getLastProposal());
  }

  /**
   * After setting last proposal,
   * concurrently clearing it will only clear it once
   * and rest of the attempts have no effect.
   */
  @Test
  void concurrentlyRepeatedlyClearLastProposal() {
    ArrayList<Thread> threads = new ArrayList<Thread>();
    final CountDownLatch lastProposalCleared = new CountDownLatch(allBlockProposals.size());
    db.setLastProposal(allBlockProposals.get(0));

    for (BlockProposal blockProposal : allBlockProposals) {
      threads.add(new Thread() {
        @Override
        public void run() {
          if (db.getLastProposal() != null) {
            db.clearLastProposal();
          } else {
            Assertions.assertDoesNotThrow(() -> db.clearLastProposal());
          }
          lastProposalCleared.countDown();
        }
      });
    }

    for (int i = 0; i < allBlockProposals.size(); i++) {
      try {
        threads.get(i).start();
      } catch (IllegalStateException e) {
        Assertions.fail(e);
      }
    }

    try {
      boolean doneOnTime = lastProposalCleared.await(1, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOnTime);
    } catch (InterruptedException e) {
      Assertions.fail(e);
    }

    Assertions.assertNull(db.getLastProposal());
  }
}
