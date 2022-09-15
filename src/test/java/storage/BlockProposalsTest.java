package storage;

import java.io.File;
import java.io.IOException;
import java.lang.IllegalStateException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.Phaser;
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
    for (int i = 0; i < 10; i++) {
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
   * If multiple threads concurrently set different proposals as last proposal
   * Only one of them will succeed and rest will throw IllegalStateException
   * Lastly the set proposal must be one of them.
   */
  @Test
  void concurrentlySetLastBlockProposal() {
    ArrayList<Thread> threads = new ArrayList<Thread>();
    final Phaser phaser = new Phaser();
    final BlockProposal[] testBlockProposal = new BlockProposal[1];

    for (BlockProposal blockProposal : allBlockProposals) {
      threads.add(new Thread() {
        @Override
        public void run() {
          phaser.register();
          setName(blockProposal.id().toString());

          phaser.arriveAndAwaitAdvance();

          try {
            db.setLastProposal(blockProposal);
            Assertions.assertEquals(db.getLastProposal().id(), blockProposal.id());
            testBlockProposal[0] = blockProposal;
          } catch (Exception e) {
            Assertions.assertEquals(e.getMessage(), BlockProposalsMapDb.LAST_BLOCK_PROPOSAL_EXISTS);
          }
        }
      });
    }

    phaser.register();
    for (int i = 0; i < 10; i++) {
      try {
        threads.get(i).start();
      } catch (Exception e) {
        System.err.println(e.getMessage());
      }
    }
    phaser.arriveAndAwaitAdvance();

    for (int i = 0; i < 10; i++) {
      try {
        threads.get(i).join();
      } catch (Exception e) {
        System.err.println(e.getMessage());
      }
    }
    Assertions.assertEquals(db.getLastProposal().id(), testBlockProposal[0].id());
  }

  /**
   * Test that the state of last proposal should not be distorted.
   * When BlockProposalsMapDb has the last proposal set,
   * 10 threads concurrently setting the last proposal,
   * thrown IllegalStateException.
   * Once all threads are done the last proposal must be the one
   * that initially was set on the database.
   */
  @Test
  void concurrentlySetExistingLastBlockProposal() {
    ArrayList<Thread> threads = new ArrayList<Thread>();
    final Phaser phaser = new Phaser();
    BlockProposal newBlockProposal = BlockFixture.newBlockProposal();

    db.clearLastProposal();
    db.setLastProposal(newBlockProposal);

    for (BlockProposal blockProposal : allBlockProposals) {
      threads.add(new Thread() {
        @Override
        public void run() {
          phaser.register();
          setName(blockProposal.id().toString());

          phaser.arriveAndAwaitAdvance();

          try {
            db.setLastProposal(blockProposal);
          } catch (Exception e) {
            Assertions.assertEquals(e.getMessage(), BlockProposalsMapDb.LAST_BLOCK_PROPOSAL_EXISTS);
          }
        }
      });
    }

    phaser.register();
    for (int i = 0; i < 10; i++) {
      try {
        threads.get(i).start();
      } catch (Exception e) {
        System.err.println(e.getMessage());
      }
    }
    phaser.arriveAndAwaitAdvance();

    for (int i = 0; i < 10; i++) {
      try {
        threads.get(i).join();
      } catch (Exception e) {
        System.err.println(e.getMessage());
      }
    }

    Assertions.assertEquals(db.getLastProposal().id(), newBlockProposal.id());
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
    db.clearLastProposal();
    Assertions.assertNull(db.getLastProposal());
  }

  /**
   * WhenBlockProposalsMapDb has the last proposal set,
   * calling clearLastProposal multiple times sequentially
   * does not have any side effects except it only cleans
   * the last proposal (once), and the rest attempts are idempotent.
   */
  @Test
  void repeatedlyClearLastProposal() {
    BlockProposal blockProposal = allBlockProposals.get(0);

    db.setLastProposal(blockProposal);
    Assertions.assertEquals(db.getLastProposal().id(), blockProposal.id());

    db.clearLastProposal();
    Assertions.assertNull(db.getLastProposal());

    for (int i = 0; i < 10; i++) {
      Assertions.assertDoesNotThrow(() -> db.clearLastProposal());
    }
  }

  /**
   * After setting last proposal,
   * concurrently clearing it will only clear it once
   * and rest of the attempts have no effect.
   */
  @Test
  void repeatedlyConcurrentlyClearLastProposal()
      throws IllegalStateException, InterruptedException {
    ArrayList<Thread> threads = new ArrayList<Thread>();
    final Phaser phaser = new Phaser();
    db.setLastProposal(allBlockProposals.get(0));

    for (BlockProposal blockProposal : allBlockProposals) {
      threads.add(new Thread() {
        @Override
        public void run() {
          phaser.register();
          setName(blockProposal.id().toString());

          phaser.arriveAndAwaitAdvance();

          if (db.getLastProposal() != null) {
            db.clearLastProposal();
          } else {
            Assertions.assertDoesNotThrow(() -> db.clearLastProposal());
          }
        }
      });
    }

    phaser.register();
    for (int i = 0; i < 10; i++) {
      try {
        threads.get(i).start();
      } catch (IllegalStateException e) {
        throw new IllegalStateException(e.getMessage());
      }
    }
    phaser.arriveAndAwaitAdvance();

    for (int i = 0; i < 10; i++) {
      try {
        threads.get(i).join();
      } catch (InterruptedException e) {
        throw new InterruptedException(e.getMessage());
      }
    }
  }
}
