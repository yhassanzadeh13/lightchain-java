package protocol.engines;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.*;

import model.Entity;
import model.codec.EntityType;
import model.crypto.PublicKey;
import model.crypto.Signature;
import model.lightchain.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import protocol.Parameters;
import protocol.assigner.AssignerInf;
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
  /**
   * Evaluates that when a new validated block arrives at ingest engine,
   * the engine adds the block to its block storage database.
   * The engine also adds hash of all the transactions of block into its "transactions" database.
   */
  @Test
  public void testValidatedSingleBlock() {
    Blocks blocks = mock(Blocks.class);
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    Block block = ValidatedBlockFixture.newValidatedBlock(accounts);

    // mocks block as new to ingest engine.
    when(seenEntities.has(block.id())).thenReturn(false);
    when(blocks.has(block.id())).thenReturn(false);

    IngestEngine ingestEngine = this.mockIngestEngineForEntities(
        new ArrayList<>(List.of(block)),
        seenEntities,
        transactionIds,
        pendingTransactions,
        blocks);

    // action
    ingestEngine.process(block);

    // verify
    verifyBlockHappyPathCalled(block, blocks, pendingTransactions, transactionIds, seenEntities);
  }

  /**
   * Evaluates that when two validated blocks arrive at ingest engine SEQUENTIALLY,
   * the engine adds the blocks to its block storage database.
   * The engine also adds hash of all the transactions of blocks into its "transactions" database.
   */
  @Test
  public void testValidatedTwoBlocks() {
    Blocks blocks = mock(Blocks.class);
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    Block block1 = ValidatedBlockFixture.newValidatedBlock(accounts);
    Block block2 = ValidatedBlockFixture.newValidatedBlock(accounts);

    IngestEngine ingestEngine = this.mockIngestEngineForEntities(
        new ArrayList<>(Arrays.asList(block1, block2)),
        seenEntities,
        transactionIds,
        pendingTransactions,
        blocks);

    // action
    ingestEngine.process(block1);
    ingestEngine.process(block2);

    // verification for block 1
    verifyBlockHappyPathCalled(block1, blocks, pendingTransactions, transactionIds, seenEntities);

    // verification for block 2
    verifyBlockHappyPathCalled(block2, blocks, pendingTransactions, transactionIds, seenEntities);
  }

  /**
   * Evaluates that when two validated blocks arrive at ingest engine concurrently,
   * the engine adds the blocks to its block storage database.
   * The engine also adds hash of all the transactions of blocks into its "transactions" database.
   */
  @Test
  public void testValidatedTwoBlocksConcurrently() {
    // R
    Blocks blocks = mock(Blocks.class);
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    Block block1 = ValidatedBlockFixture.newValidatedBlock(accounts);
    Block block2 = ValidatedBlockFixture.newValidatedBlock(accounts);

    IngestEngine ingestEngine = this.mockIngestEngineForEntities(
        new ArrayList<>(Arrays.asList(block1, block2)),
        seenEntities,
        transactionIds,
        pendingTransactions,
        blocks);

    this.processEntitiesConcurrently(
        ingestEngine,
        new ArrayList<>(Arrays.asList(block1, block2)));

    // verification for block 1
    verifyBlockHappyPathCalled(block1, blocks, pendingTransactions, transactionIds, seenEntities);

    // verification for block 2
    verifyBlockHappyPathCalled(block2, blocks, pendingTransactions, transactionIds, seenEntities);
  }

  /**
   * Evaluates that when two same validated blocks arrive at ingest engine (second one should be ignored),
   * the engine adds the blocks to its block storage database.
   * The engine also adds hash of all the transactions of blocks into its "transactions" database.
   */
  @Test
  public void testValidatedSameTwoBlocks() {
    // R
    Blocks blocks = mock(Blocks.class);
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    Block block = ValidatedBlockFixture.newValidatedBlock(accounts);

    when(seenEntities.has(block.id())).thenReturn(false);
    when(blocks.has(block.id())).thenReturn(false);

    IngestEngine ingestEngine = this.mockIngestEngineForEntities(
        new ArrayList<>(List.of(block)),
        seenEntities,
        transactionIds,
        pendingTransactions,
        blocks);

    // action
    ingestEngine.process(block);
    when(seenEntities.has(block.id())).thenReturn(true); // block is already seen
    ingestEngine.process(block);

    // verification
    verifyBlockHappyPathCalled(block, blocks, pendingTransactions, transactionIds, seenEntities);
    verify(seenEntities, times(2)).has(block.id());
  }

  /**
   * Evaluates that when a new validated block (with shared transactions in pendingTx) arrive at ingest engine,
   * the engine adds the blocks to its block storage database.
   * The engine also removes hash of the transactions of blocks from pendingTransactions.
   */
  @Test
  public void testValidatedBlockContainingPendingTransaction() {
    // R
    Blocks blocks = mock(Blocks.class);
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    Block block = ValidatedBlockFixture.newValidatedBlock(accounts);

    IngestEngine ingestEngine = this.mockIngestEngineForEntities(
        new ArrayList<>(List.of(block)),
        seenEntities,
        transactionIds,
        pendingTransactions,
        blocks);

    for (Transaction tx : block.getTransactions()) {
      when(pendingTransactions.has(tx.id())).thenReturn(true); // pendingTx contains all txs of block
    }

    // action
    ingestEngine.process(block);

    // verification
    verifyBlockHappyPathCalled(block, blocks, pendingTransactions, transactionIds, seenEntities);
    for (Transaction tx : block.getTransactions()) {
      // all transactions included in the block must be removed from pending transactions.
      verify(pendingTransactions, times(1)).remove(tx.id());
    }
  }

  /**
   * Evaluates that when two new validated blocks (with a shared transactions in pendingTx, disjoint set)
   * arrive at ingest engine, the engine adds the blocks to its block storage database.
   * The engine also removes the hash of the single shared transaction among blocks from pending transactions.
   */
  @Test
  public void testConcurrentBlockIngestionContainingSeenTransactionDisjointSet() {
    // R
    Blocks blocks = mock(Blocks.class);
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    Block block1 = ValidatedBlockFixture.newValidatedBlock(accounts);
    Block block2 = ValidatedBlockFixture.newValidatedBlock(accounts);

    IngestEngine ingestEngine = this.mockIngestEngineForEntities(
        new ArrayList<>(Arrays.asList(block1, block2)),
        seenEntities,
        transactionIds,
        pendingTransactions,
        blocks);

    // simulates 1 shared transaction for each block
    when(pendingTransactions.has(block1.getTransactions()[0].id())).thenReturn(true);
    when(pendingTransactions.has(block2.getTransactions()[0].id())).thenReturn(true);

    processEntitiesConcurrently(
        ingestEngine,
        new ArrayList<>(Arrays.asList(block1, block2)));

    // verification for block1
    verifyBlockHappyPathCalled(block1, blocks, pendingTransactions, transactionIds, seenEntities);
    // shared transaction should be removed from pending transactions
    verify(pendingTransactions, times(1)).remove(block1.getTransactions()[0].id());

    // verification for block2
    verifyBlockHappyPathCalled(block2, blocks, pendingTransactions, transactionIds, seenEntities);
    // shared transaction should be removed from pending transactions
    verify(pendingTransactions, times(1)).remove(block2.getTransactions()[0].id());
  }

  /**
   * Evaluates that when two new validated blocks (with shared transactions in pendingTx, overlapping set)
   * arrive at ingest engine, the engine adds the blocks to its block storage database.
   * The engine also removes hash of the transactions of blocks from pendingTransactions.
   */
  @Test
  public void testConcurrentBlockIngestionContainingSeenTransactionOverlappingSet() {
    // R
    Blocks blocks = mock(Blocks.class);
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    Block block1 = ValidatedBlockFixture.newValidatedBlock(accounts);
    Block block2 = ValidatedBlockFixture.newValidatedBlock(accounts);

    IngestEngine ingestEngine = this.mockIngestEngineForEntities(
        new ArrayList<>(Arrays.asList(block1, block2)),
        seenEntities,
        transactionIds,
        pendingTransactions,
        blocks);

    // simulates an overlapping set of shared transactions
    when(pendingTransactions.has(any(Identifier.class))).thenReturn(true);

    processEntitiesConcurrently(
        ingestEngine,
        new ArrayList<>(Arrays.asList(block1, block2)));

    // verification for block1
    verifyBlockHappyPathCalled(block1, blocks, pendingTransactions, transactionIds, seenEntities);
    for (Transaction tx : block1.getTransactions()) {
      verify(pendingTransactions, times(1)).remove(tx.id());
    }

    // verification for block2
    verifyBlockHappyPathCalled(block2, blocks, pendingTransactions, transactionIds, seenEntities);
    for (Transaction tx : block2.getTransactions()) {
      verify(pendingTransactions, times(1)).remove(tx.id());
    }
  }

  /**
   * Evaluates that when an already ingested validated block arrives at ingest engine,
   * the engine discards the block right away.
   */
  @Test
  public void testValidatedAlreadyIngestedBlock() {
    // R
    Blocks blocks = mock(Blocks.class);
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    Block block = ValidatedBlockFixture.newValidatedBlock(accounts);

    IngestEngine ingestEngine = this.mockIngestEngineForEntities(
        new ArrayList<>(List.of(block)),
        seenEntities,
        transactionIds,
        pendingTransactions,
        blocks);

    when(seenEntities.has(block.id())).thenReturn(true); // block is already ingested
    when(blocks.has(block.id())).thenReturn(true); // block is already ingested

    // action
    ingestEngine.process(block);

    // verification
    verify(blocks, times(0)).add(block);
    verify(seenEntities, times(0)).add(block.id());
    verify(seenEntities, times(1)).has(block.id());
    for (Transaction tx : block.getTransactions()) {
      verify(pendingTransactions, times(0)).has(tx.id());
      verify(transactionIds, times(0)).add(tx.id());
    }
  }

  /**
   * Evaluates that when a new validated transaction arrives at ingest engine,
   * the engine adds hash of the transaction into its pending transactions' database.
   */
  @Test
  public void testValidatedTransaction() {
    // R
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);
    Blocks blocks = mock(Blocks.class);

    ValidatedTransaction tx = ValidatedTransactionFixture.newValidatedTransaction();

    IngestEngine ingestEngine = this.mockIngestEngineForEntities(
        new ArrayList<>(List.of(tx)),
        seenEntities,
        transactionIds,
        pendingTransactions,
        blocks);

    // action
    ingestEngine.process(tx);

    // verification
    verifyTransactionHappyPathCalled(tx, seenEntities, transactionIds, pendingTransactions);
  }

  /**
   * Evaluates that when two validated transactions arrives at ingest engine sequentially,
   * the engine adds the hashes of the transactions into its pending transactions' database.
   */
  @Test
  public void testValidatedTwoTransactions() {
    // R
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);
    Blocks blocks = mock(Blocks.class);

    ValidatedTransaction tx1 = ValidatedTransactionFixture.newValidatedTransaction();
    ValidatedTransaction tx2 = ValidatedTransactionFixture.newValidatedTransaction();
    when(seenEntities.has(any(Identifier.class))).thenReturn(false);
    when(transactionIds.has(any(Identifier.class))).thenReturn(false);
    when(pendingTransactions.has(any(Identifier.class))).thenReturn(false);

    IngestEngine ingestEngine = this.mockIngestEngineForEntities(
        new ArrayList<>(Arrays.asList(tx1, tx2)),
        seenEntities,
        transactionIds,
        pendingTransactions,
        blocks);

    // action
    ingestEngine.process(tx1);
    ingestEngine.process(tx2);

    // verification of tx1
    verifyTransactionHappyPathCalled(tx1, seenEntities, transactionIds, pendingTransactions);

    // verification of tx2
    verifyTransactionHappyPathCalled(tx2, seenEntities, transactionIds, pendingTransactions);
  }

  /**
   * Evaluates that when two validated transactions arrive at ingest engine concurrently,
   * the engine adds the hashes of the transactions into its pending transactions' database.
   */
  @Test
  public void testConcurrentValidatedTwoTransactions() {
    // R
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);
    Blocks blocks = mock(Blocks.class);

    ValidatedTransaction tx1 = ValidatedTransactionFixture.newValidatedTransaction();
    ValidatedTransaction tx2 = ValidatedTransactionFixture.newValidatedTransaction();

    IngestEngine ingestEngine = this.mockIngestEngineForEntities(
        new ArrayList<>(Arrays.asList(tx1, tx2)),
        seenEntities,
        transactionIds,
        pendingTransactions,
        blocks);

    processEntitiesConcurrently(
        ingestEngine,
        new ArrayList<>(List.of(tx1, tx2)));

    // verification of tx1
    verifyTransactionHappyPathCalled(tx1, seenEntities, transactionIds, pendingTransactions);

    // verification of tx2
    verifyTransactionHappyPathCalled(tx2, seenEntities, transactionIds, pendingTransactions);
  }

  /**
   * Evaluates that when two same validated transactions arrive at ingest engine sequentially,
   * the engine adds the hash of the first transaction into its pending transactions' database.
   * The engine also discards the second transaction right away.
   */
  @Test
  public void testValidatedSameTwoTransactions() {
    // R
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);
    Blocks blocks = mock(Blocks.class);

    ValidatedTransaction tx = ValidatedTransactionFixture.newValidatedTransaction();
    when(seenEntities.has(tx.id())).thenReturn(false);
    when(transactionIds.has(tx.id())).thenReturn(false);
    when(pendingTransactions.has(tx.id())).thenReturn(false);

    IngestEngine ingestEngine = this.mockIngestEngineForEntities(
        new ArrayList<>(List.of(tx)),
        seenEntities,
        transactionIds,
        pendingTransactions,
        blocks);

    // action
    ingestEngine.process(tx);
    when(seenEntities.has(tx.id())).thenReturn(true);
    when(pendingTransactions.has(tx.id())).thenReturn(true);
    // process the same transaction again.
    ingestEngine.process(tx);

    // verification
    verifyTransactionHappyPathCalled(tx, seenEntities, transactionIds, pendingTransactions);
    verify(seenEntities, times(2)).has(tx.id());
  }

  /**
   * Evaluates that when a validated transaction which is already in transaction identifiers arrives at ingest engine,
   * the engine discards the transaction.
   */
  @Test
  public void testValidatedTransactionAlreadyInTransactionIdStorage() {
    // R
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);
    Blocks blocks = mock(Blocks.class);

    ValidatedTransaction tx = ValidatedTransactionFixture.newValidatedTransaction();

    final IngestEngine ingestEngine = this.mockIngestEngineForEntities(
        new ArrayList<>(List.of(tx)),
        seenEntities,
        transactionIds,
        pendingTransactions,
        blocks);

    // transaction is not seen, but it is in transaction ids storage (as the result of processing a validated block).
    when(seenEntities.has(tx.id())).thenReturn(false);
    when(transactionIds.has(tx.id())).thenReturn(true);
    when(pendingTransactions.has(tx.id())).thenReturn(false);

    // action
    ingestEngine.process(tx);

    // verification
    verify(seenEntities, times(1)).add(tx.id());
    verify(pendingTransactions, times(1)).has(tx.id());
    verify(transactionIds, times(1)).has(tx.id());
    verify(pendingTransactions, times(0)).add(tx);
  }

  /**
   * Evaluates that when an entity that is neither a validated block nor a validated transaction
   * arrives at ingest engine, the engine throws IllegalArgumentException.
   */
  @Test
  public void testNeitherBlockNorTransaction() {
    // R
    Transactions pendingTransactions = mock(Transactions.class);
    AssignerInf assigner = mock(AssignerInf.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Blocks blocks = mock(Blocks.class);
    State state = mock(State.class);
    Identifiers seenEntities = mock(Identifiers.class);

    Entity e = new EntityFixture(); // not a block nor a transaction
    IngestEngine ingestEngine = new IngestEngine(
        state,
        blocks,
        transactionIds,
        pendingTransactions,
        seenEntities,
        assigner);
    Assertions.assertThrows(IllegalArgumentException.class, () -> ingestEngine.process(e));
  }

  /**
   * Evaluates that when a validated block and a validated transaction arrives at ingest engine concurrently,
   * the engine adds the block to its block storage database.
   * The engine also adds hash of all the transactions of block
   * and the hash of the validated transaction into its "transactions" database.
   */
  @Test
  public void testConcurrentValidatedTransactionAndBlockNonOverlapping() {
    // R
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);
    Blocks blocks = mock(Blocks.class);

    ValidatedTransaction validatedTx = ValidatedTransactionFixture.newValidatedTransaction();
    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    Block block = ValidatedBlockFixture.newValidatedBlock(accounts);

    IngestEngine ingestEngine = this.mockIngestEngineForEntities(
        new ArrayList<>(Arrays.asList(block, validatedTx)),
        seenEntities,
        transactionIds,
        pendingTransactions,
        blocks);

    processEntitiesConcurrently(
        ingestEngine,
        new ArrayList<>(List.of(block, validatedTx)));

    // verification for block
    verifyBlockHappyPathCalled(block, blocks, pendingTransactions, transactionIds, seenEntities);

    // verification for transaction
    verifyTransactionHappyPathCalled(validatedTx, seenEntities, transactionIds, pendingTransactions);
    // no transaction should be removed from pending ones
    verify(pendingTransactions, times(0)).remove(any(Identifier.class));
  }

  /**
   * Evaluates that when a validated block and a validated transaction (which the block contains)
   * arrive at ingest engine (block first), the engine adds the block to its block storage database.
   * The engine also adds hash of all the transactions of block into its "transactions" database.
   * Hence, when the transaction that also included in the block comes next, it is never added to pending
   * transactions' database.
   */
  @Test
  public void testProcessBlockAndIncludedTransaction_BlockFirst() {
    // R
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);
    Blocks blocks = mock(Blocks.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    Block block = ValidatedBlockFixture.newValidatedBlock(accounts);
    ValidatedTransaction validatedTx = block.getTransactions()[0]; // the transaction is in the block

    IngestEngine ingestEngine = this.mockIngestEngineForEntities(
        new ArrayList<>(Arrays.asList(block, validatedTx)),
        seenEntities,
        transactionIds,
        pendingTransactions,
        blocks);

    // process block
    ingestEngine.process(block);
    // as result of processing block, the validated transaction that also included in the block is
    // added to transaction ids.
    when(transactionIds.has(validatedTx.id())).thenReturn(true);
    // process transaction
    ingestEngine.process(validatedTx);

    // verification for block
    verify(blocks, times(1)).add(block);
    verify(seenEntities, times(1)).add(block.id());
    for (Transaction tx : block.getTransactions()) {
      verify(transactionIds, times(1)).add(tx.id());
    }

    // verification for transaction
    verify(seenEntities, times(1)).add(validatedTx.id());
    verify(transactionIds, times(1)).has(validatedTx.id());
    verify(pendingTransactions, times(0)).add(validatedTx);
  }

  /**
   * Evaluates that when a validated block and a validated transaction (which the block contains)
   * arrive at ingest engine (transaction first), the engine adds the block to its block storage database.
   * The engine also adds hash of all the transactions of block into its "transactions" database.
   * Hence, when the transaction that also included in the block comes next, it is never added to pending
   * transactions' database.
   */
  @Test
  public void testProcessBlockAndIncludedTransaction_TransactionFirst() {
    // R
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);
    Blocks blocks = mock(Blocks.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    Block block = ValidatedBlockFixture.newValidatedBlock(accounts);
    ValidatedTransaction validatedTx = block.getTransactions()[0]; // the transaction is in the block

    IngestEngine ingestEngine = this.mockIngestEngineForEntities(
        new ArrayList<>(Arrays.asList(block, validatedTx)),
        seenEntities,
        transactionIds,
        pendingTransactions,
        blocks);

    // process transaction first.
    ingestEngine.process(validatedTx);
    // as result of processing transaction, the transaction must also be added to transaction ids.
    when(transactionIds.has(validatedTx.id())).thenReturn(true);
    // after processing transaction it should be added to pending transaction
    when(pendingTransactions.has(validatedTx.id())).thenReturn(true);
    // process block next.
    ingestEngine.process(block);

    // verification for block
    verify(blocks, times(1)).add(block);
    verify(seenEntities, times(1)).add(block.id());
    for (Transaction tx : block.getTransactions()) {
      verify(transactionIds, times(1)).add(tx.id());
    }

    // verification for transaction
    verify(seenEntities, times(1)).add(validatedTx.id());
    verify(transactionIds, times(1)).has(validatedTx.id());
    // transaction must be added to pending ones, and then removed.
    verify(pendingTransactions, times(1)).add(validatedTx);
    verify(pendingTransactions, times(1)).remove(validatedTx.id());
  }

  /**
   * Verifies mocked storage components have been called with expected parameters on an
   * expected number of times for block happy path, i.e., block is added to the blocks storage, and its id is
   * added to seenEntities storage. Also, all its transactions ids are added to the pendingTx and
   * txIds.
   *
   * @param block        the block itself.
   * @param blocks       the blocks storage component.
   * @param pendingTx    the pending transactions identifiers.
   * @param txIds        the transaction identifiers.
   * @param seenEntities identifiers of processed entities by engine.
   */
  private void verifyBlockHappyPathCalled(
      Block block,
      Blocks blocks,
      Transactions pendingTx,
      Identifiers txIds,
      Identifiers seenEntities) {

    verify(blocks, times(1)).add(block);
    verify(seenEntities, times(1)).add(block.id());
    for (Transaction tx : block.getTransactions()) {
      verify(pendingTx, times(1)).has(tx.id());
      verify(txIds, times(1)).add(tx.id());
    }
  }

  /**
   * Verifies mocked storage components have been called with the expected parameters on an
   * expected number of times for transaction happy path.
   *
   * @param transaction  the transaction itself.
   * @param seenEntities identifiers of processed entities by engine.
   * @param txIds        the transaction identifiers.
   * @param pendingTx    the pending transactions identifiers.
   */
  private void verifyTransactionHappyPathCalled(
      Transaction transaction,
      Identifiers seenEntities,
      Identifiers txIds,
      Transactions pendingTx) {

    verify(seenEntities, times(1)).add(transaction.id());
    verify(txIds, times(1)).has(transaction.id());
    verify(pendingTx, times(1)).add(transaction);
  }

  /**
   * Sets up the mocks for ingest engine for processing given list of entities.
   *
   * @param entities     list of entities.
   * @param seenEntities identifiers of processed entities by engine.
   * @param txIds        identifiers of transactions.
   * @param pendingTx    identifiers of pending transactions.
   * @param blocks       the blocks storage component.
   * @return mocked ingest engine with mocked components.
   */
  private IngestEngine mockIngestEngineForEntities(
      ArrayList<Entity> entities,
      Identifiers seenEntities,
      Identifiers txIds,
      Transactions pendingTx,
      Blocks blocks) {

    Snapshot snapshot = mock(Snapshot.class);
    AssignerInf assigner = mock(AssignerInf.class);
    State state = mock(State.class);

    for (Entity e : entities) {
      // mocks entity as new
      when(seenEntities.has(e.id())).thenReturn(false);

      if (e.type().equals(EntityType.TYPE_VALIDATED_TRANSACTION)) {

        ValidatedTransaction tx = (ValidatedTransaction) e;
        when(state.atBlockId(tx.getReferenceBlockId())).thenReturn(snapshot);
        when(pendingTx.has(tx.id())).thenReturn(false);
        when(txIds.has(tx.id())).thenReturn(false);

      } else if (e.type().equals(EntityType.TYPE_VALIDATED_BLOCK)) {

        ValidatedBlock block = (ValidatedBlock) e;
        when(state.atBlockId(block.getPreviousBlockId())).thenReturn(snapshot);
        when(blocks.has(block.id())).thenReturn(false);
        for (Transaction tx : block.getTransactions()) {
          when(pendingTx.has(tx.id())).thenReturn(false);
        }
      }

      // mocks assignment
      mockAssignment(assigner, e, snapshot);
    }

    return new IngestEngine(
        state,
        blocks,
        txIds,
        pendingTx,
        seenEntities,
        assigner);
  }

  /**
   * Processes entities concurrently thorough ingest engine.
   *
   * @param ingestEngine ingest engine.
   * @param entities     the entities to be processed.
   */
  private void processEntitiesConcurrently(
      IngestEngine ingestEngine,
      ArrayList<Entity> entities) {

    AtomicInteger threadError = new AtomicInteger();
    int concurrencyDegree = entities.size();
    CountDownLatch threadsDone = new CountDownLatch(concurrencyDegree);
    Thread[] threads = new Thread[concurrencyDegree];

    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      threads[i] = new Thread(() -> {
        try {
          ingestEngine.process(entities.get(finalI));
          threadsDone.countDown();
        } catch (IllegalStateException e) {
          threadError.getAndIncrement();
        }
      });
    }
    for (Thread t : threads) {
      t.start();
    }
    try {
      boolean doneOneTime = threadsDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    Assertions.assertEquals(0, threadError.get());
  }

  /**
   * Mocks assignment of for an entity.
   *
   * @param assigner the validator assigner.
   * @param e        the entity to be assigned.
   * @param snapshot the snapshot.
   */
  private void mockAssignment(AssignerInf assigner, Entity e, Snapshot snapshot) {
    Assignment assignment = mock(Assignment.class);
    when(assigner.assign(e.id(), snapshot, Parameters.VALIDATOR_THRESHOLD)).thenReturn(assignment);
    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all identifiers
    PublicKey pubKey = mock(PublicKey.class); // mock public key
    Account account = mock(Account.class); // mock account
    when(account.getPublicKey()).thenReturn(pubKey); // returns the mocked public key for all accounts

    // returns true for all signatures
    when(pubKey.verifySignature(any(Block.class), any(Signature.class))).thenReturn(true);
    when(pubKey.verifySignature(any(Transaction.class), any(Signature.class))).thenReturn(true);
    // returns the mock account for all identifiers
    when(snapshot.getAccount(any(Identifier.class))).thenReturn(account);
  }
}