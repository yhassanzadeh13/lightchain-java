package protocol.engines;

import java.util.ArrayList;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import protocol.Parameters;
import protocol.assigner.ValidatorAssigner;
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

  /**
   * Evaluates that when a new validated transaction arrives at ingest engine,
   * the engine adds hash of the transaction into its "transactions" database.
   */
  @Test
  public void testValidatedTransaction() {

    while (true) {

    try {
      Thread.sleep(1000);
    } catch (InterruptedException ex) {
      System.err.println("Thread sleep issue, breaking the loop");
      break;
    }

    Blocks blocks = mock(Blocks.class);
    Snapshot snapshot = mock(Snapshot.class);
    ValidatorAssigner assigner = mock(ValidatorAssigner.class);
    Transactions pendingTransactions = mock(Transactions.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }
    ValidatedTransaction tx = ValidatedTransactionFixture.newValidatedTransaction();
    when(snapshot.all()).thenReturn(accounts);

    Identifiers seenEntities = mock(Identifiers.class);
    when(seenEntities.has(tx.id())).thenReturn(false);

    State state = mock(State.class);
    when(state.atBlockId(tx.getReferenceBlockId())).thenReturn(snapshot);

    Identifiers transactionIds = mock(Identifiers.class);
    when(transactionIds.has(tx.id())).thenReturn(false);

    IngestEngine ingestEngine = new IngestEngine(
            state,
            blocks,
            transactionIds,
            pendingTransactions,
            seenEntities,
            assigner,
            pendingTransactionAdditions,
            validBlocks,
            validTransactions);

    // mocks assignment
    Assignment assignment = mock(Assignment.class);
    when(assigner.assign(tx.id(), snapshot, Parameters.VALIDATOR_THRESHOLD)).thenReturn(assignment);
    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all identifiers
    PublicKey pubKey = mock(PublicKey.class); // mock public key
    Account account = mock(Account.class); // mock account
    when(account.getPublicKey()).thenReturn(pubKey); // returns the mocked public key for all accounts

    // returns true for all signatures
    when(pubKey.verifySignature(any(Transaction.class), any(Signature.class))).thenReturn(true);
    // returns the mock account for all identifiers
    when(snapshot.getAccount(any(Identifier.class))).thenReturn(account);

    // action
    ingestEngine.process(tx);

    // verification
    verify(seenEntities, times(1)).add(tx.id());
    verify(transactionIds, times(1)).has(tx.id());
    verify(pendingTransactions, times(1)).add(tx);


       pendingTransactions = mock(Transactions.class);
       snapshot = mock(Snapshot.class);
       assigner = mock(ValidatorAssigner.class);
       transactionIds = mock(Identifiers.class);

      accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
      for (Account account2 : accounts) {
        when(snapshot.getAccount(account2.getIdentifier())).thenReturn(account);
      }
      Block block = ValidatedBlockFixture.newValidatedBlock(accounts);
      when(snapshot.all()).thenReturn(accounts);

       seenEntities = mock(Identifiers.class);
      when(seenEntities.has(block.id())).thenReturn(false);

       blocks = mock(Blocks.class);
      when(blocks.has(block.id())).thenReturn(false);

      for (Transaction tx2 : block.getTransactions()) {
        when(pendingTransactions.has(tx2.id())).thenReturn(false);
      }

       state = mock(State.class);
      when(state.atBlockId(block.getPreviousBlockId())).thenReturn(snapshot);

       ingestEngine = new IngestEngine(
              state,
              blocks,
              transactionIds,
              pendingTransactions,
              seenEntities,
              assigner,
              pendingTransactionAdditions,
              validBlocks,
              validTransactions);

      // mocks assignment
       assignment = mock(Assignment.class);
      when(assigner.assign(block.id(), snapshot, Parameters.VALIDATOR_THRESHOLD)).thenReturn(assignment);
      when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all identifiers
       pubKey = mock(PublicKey.class); // mock public key
       account = mock(Account.class); // mock account
      when(account.getPublicKey()).thenReturn(pubKey); // returns the mocked public key for all accounts

      // returns true for all signatures
      when(pubKey.verifySignature(any(Block.class), any(Signature.class))).thenReturn(true);
      // returns the mock account for all identifiers
      when(snapshot.getAccount(any(Identifier.class))).thenReturn(account);

      // action
      ingestEngine.process(block);

      // verification
      verify(blocks, times(1)).add(block);
      verify(seenEntities, times(1)).add(block.id());
      for (Transaction tx3 : block.getTransactions()) {
        verify(pendingTransactions, times(1)).has(tx3.id());
        verify(transactionIds, times(1)).add(tx3.id());
      }

    }

    }
  }
