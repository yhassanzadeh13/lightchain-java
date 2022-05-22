package protocol.engines;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.*;

import io.prometheus.client.Counter;
import metrics.collectors.LightChainCollector;
import metrics.collectors.MetricServer;
import metrics.integration.MetricsTestNet;
import model.Entity;
import model.crypto.PublicKey;
import model.crypto.Signature;
import model.lightchain.*;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import protocol.Parameters;
import state.Snapshot;
import state.State;
import storage.Blocks;
import storage.Identifiers;
import storage.Transactions;
import unittest.fixtures.AccountFixture;
import unittest.fixtures.EntityFixture;
import unittest.fixtures.ValidatedBlockFixture;
import unittest.fixtures.ValidatedTransactionFixture;

/**
 * Encapsulates tests for ingest engine implementation.
 */
public class IngestEngineTest {

  // TODO: a single individual test function for each of these scenarios:
  // 1. Happy path of receiving a validated single block.
  // 2. Happy path of receiving two validated blocks sequentially.
  // 3. Happy path of receiving two validated blocks concurrently.
  // 4. Happy path of receiving two duplicate validated blocks sequentially
  //    (the second duplicate block should be discarded).
  // 5. Happy path of receiving two duplicate validated blocks concurrently
  //    (the second duplicate block should be discarded).
  // 6. Happy path of receiving a validated block with shared transactions in pendingTx.
  // 7. Happy path of receiving two validated blocks concurrently that each have some transactions in pendingTx
  //    (disjoint sets of transactions).
  // 8. Happy path of receiving two validated blocks concurrently that each have some transactions in pendingTx
  //    (overlapping sets of transactions).
  // 9. Happy path of receiving an already ingested validated block (i.e., block already added to blocks database),
  //    second block should be discarded right away.
  // 10. Happy path of receiving a validated transaction.
  // 11. Happy path of receiving two validated transactions sequentially.
  // 12. Happy path of receiving two validated transactions concurrently.
  // 13. Happy path of receiving a duplicate pair of validated transactions sequentially.
  // 14. Happy path of receiving two duplicate pair of validated transactions concurrently.
  // 15. Happy path of receiving a validated transaction that its id already exists in txHash.
  // 16. Happy path of receiving a validated transaction that its id already exists in pendingTx.
  // 17. Unhappy path of receiving an entity that is neither a validated block nor a validated transaction.
  // 18. Happy path of receiving a validated transaction and a validated block concurrently
  //     (block does not contain that transaction).
  // 19. Happy path of receiving a validated transaction and a validated block concurrently
  //     (block does contain the transaction).

  private static final Random random = new Random();
  private IngestEngine ingestEngine;
  private ValidatedBlock block1;
  private ValidatedBlock block2;
  private Blocks blocks;

  private Identifiers transactionIds;
  private Transactions pendingTransactions;

  private Identifiers seenEntities;
  private Snapshot snapshot;
  private State state;
  private ArrayList<Block> blockList;
  private Assignment assignment;


  static LightChainCollector collector;
  static Counter pendingTransactionAdditions;
  static Counter validBlocks;
  static Counter validTransactions;

  @BeforeAll
  public static void set() {

    try {
      MetricServer server = new MetricServer();
      server.start();
    } catch (IllegalStateException ex) {
      throw new IllegalStateException("could not start the Metric Server", ex);
    }

    try {
      collector = new LightChainCollector();
      // possibly change the namespace and subsystem values
      pendingTransactionAdditions = collector.counter().register("additions_to_pending_count",
              "consensus", "IngestEngine", "Counter for validated transactions added to pending transactions storage");
      validBlocks = collector.counter().register("valid_block_received_count",
              "consensus", "IngestEngine", "Number of validated blocks received");
      validTransactions = collector.counter().register("valid_received_count",
              "consensus", "IngestEngine", "Number of validated transactions received");

    } catch (IllegalArgumentException ex) {
      throw new IllegalStateException("could not initialize the metrics with the provided arguments", ex);
    }

    try {
      MetricsTestNet testNet = new MetricsTestNet();
      testNet.runMetricsTestNet();
    } catch (IllegalStateException e) {
      System.err.println("could not run metrics testnet" + e);
      System.exit(1);
    }

  }


  @AfterAll
  public static void tearDown() {

    while(true);

  }

}
