package storage;

import model.lightchain.BlockProposal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import storage.mapdb.BlockProposalsMapDb;
import unittest.fixtures.BlockFixture;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

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
   *
   */
  @Test
  void setAndCheckLastBlockProposal() {
    for (BlockProposal blockProposal: allBlockProposals){
      System.out.println(blockProposal);
    }

    for (BlockProposal blockProposal: allBlockProposals){
      db.clearLastProposal();
      db.setLastProposal(blockProposal);
      Assertions.assertEquals(blockProposal.id(), db.getLastProposal().id());
    }
  }
}
