package protocol.engines;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.*;

import model.Entity;
import model.crypto.PublicKey;
import model.crypto.Signature;
import model.lightchain.*;
import org.junit.jupiter.api.Assertions;
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
  /**
   * Evaluates that when a new validated block arrives at ingest engine,
   * the engine adds the block to its block storage database.
   * The engine also adds hash of all the transactions of block into its "transactions" database.
   */
  @Test
  public void testValidatedSingleBlock() {
    Transactions pendingTransactions = mock(Transactions.class);
    Snapshot snapshot = mock(Snapshot.class);
    ValidatorAssigner assigner = mock(ValidatorAssigner.class);
    Identifiers transactionIds = mock(Identifiers.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }
    Block block = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(snapshot.all()).thenReturn(accounts);

    Identifiers seenEntities = mock(Identifiers.class);
    when(seenEntities.has(block.id())).thenReturn(false);

    Blocks blocks = mock(Blocks.class);
    when(blocks.has(block.id())).thenReturn(false);

    for (Transaction tx : block.getTransactions()) {
      when(pendingTransactions.has(tx.id())).thenReturn(false);
    }

    State state = mock(State.class);
    when(state.atBlockId(block.getPreviousBlockId())).thenReturn(snapshot);

    IngestEngine ingestEngine = new IngestEngine(
        state,
        blocks,
        transactionIds,
        pendingTransactions,
        seenEntities,
        assigner);

    // mocks assignment
    Assignment assignment = mock(Assignment.class);
    when(assigner.assign(block.id(), snapshot, Parameters.VALIDATOR_THRESHOLD)).thenReturn(assignment);
    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all identifiers
    PublicKey pubKey = mock(PublicKey.class); // mock public key
    Account account = mock(Account.class); // mock account
    when(account.getPublicKey()).thenReturn(pubKey); // returns the mocked public key for all accounts

    // returns true for all signatures
    when(pubKey.verifySignature(any(Block.class), any(Signature.class))).thenReturn(true);
    // returns the mock account for all identifiers
    when(snapshot.getAccount(any(Identifier.class))).thenReturn(account);

    // action
    ingestEngine.process(block);

    // verify
    verifyBlockHappyPathCalled(block, blocks, pendingTransactions, transactionIds, seenEntities);
  }

  /**
   * Evaluates that when two validated blocks arrive at ingest engine,
   * the engine adds the blocks to its block storage database.
   * The engine also adds hash of all the transactions of blocks into its "transactions" database.
   */
  @Test
  public void testValidatedTwoBlocks() {
    Transactions pendingTransactions = mock(Transactions.class);
    Snapshot snapshot = mock(Snapshot.class);
    ValidatorAssigner assigner = mock(ValidatorAssigner.class);
    Identifiers transactionIds = mock(Identifiers.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }
    // first block
    Block block1 = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(snapshot.all()).thenReturn(accounts);

    // second block
    Block block2 = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(snapshot.all()).thenReturn(accounts);

    Identifiers seenEntities = mock(Identifiers.class);
    when(seenEntities.has(block1.id())).thenReturn(false);
    when(seenEntities.has(block2.id())).thenReturn(false);

    Blocks blocks = mock(Blocks.class);
    when(blocks.has(block1.id())).thenReturn(false);
    when(blocks.has(block2.id())).thenReturn(false);

    for (Transaction tx : block1.getTransactions()) {
      when(pendingTransactions.has(tx.id())).thenReturn(false);
    }
    for (Transaction tx : block2.getTransactions()) {
      when(pendingTransactions.has(tx.id())).thenReturn(false);
    }

    State state = mock(State.class);
    when(state.atBlockId(block1.getPreviousBlockId())).thenReturn(snapshot);
    when(state.atBlockId(block2.getPreviousBlockId())).thenReturn(snapshot);

    IngestEngine ingestEngine = new IngestEngine(
        state,
        blocks,
        transactionIds,
        pendingTransactions,
        seenEntities,
        assigner);

    // mocks assignment
    Assignment assignment = mock(Assignment.class);
    when(assigner.assign(block1.id(), snapshot, Parameters.VALIDATOR_THRESHOLD)).thenReturn(assignment);
    when(assigner.assign(block2.id(), snapshot, Parameters.VALIDATOR_THRESHOLD)).thenReturn(assignment);
    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all identifiers
    PublicKey pubKey = mock(PublicKey.class); // mock public key
    Account account = mock(Account.class); // mock account
    when(account.getPublicKey()).thenReturn(pubKey); // returns the mocked public key for all accounts

    // returns true for all signatures
    when(pubKey.verifySignature(any(Block.class), any(Signature.class))).thenReturn(true);
    // returns the mock account for all identifiers
    when(snapshot.getAccount(any(Identifier.class))).thenReturn(account);

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
    Transactions pendingTransactions = mock(Transactions.class);
    Snapshot snapshot = mock(Snapshot.class);
    ValidatorAssigner assigner = mock(ValidatorAssigner.class);
    Identifiers transactionIds = mock(Identifiers.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }
    // first block
    Block block1 = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(snapshot.all()).thenReturn(accounts);

    // second block
    Block block2 = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(snapshot.all()).thenReturn(accounts);

    Identifiers seenEntities = mock(Identifiers.class);
    when(seenEntities.has(block1.id())).thenReturn(false);
    when(seenEntities.has(block2.id())).thenReturn(false);

    Blocks blocks = mock(Blocks.class);
    when(blocks.has(block1.id())).thenReturn(false);
    when(blocks.has(block2.id())).thenReturn(false);

    for (Transaction tx : block1.getTransactions()) {
      when(pendingTransactions.has(tx.id())).thenReturn(false);
    }
    for (Transaction tx : block2.getTransactions()) {
      when(pendingTransactions.has(tx.id())).thenReturn(false);
    }

    State state = mock(State.class);
    when(state.atBlockId(block1.getPreviousBlockId())).thenReturn(snapshot);
    when(state.atBlockId(block2.getPreviousBlockId())).thenReturn(snapshot);

    IngestEngine ingestEngine = new IngestEngine(
        state,
        blocks,
        transactionIds,
        pendingTransactions,
        seenEntities,
        assigner);

    // mocks assignment
    Assignment assignment = mock(Assignment.class);
    when(assigner.assign(block1.id(), snapshot, Parameters.VALIDATOR_THRESHOLD)).thenReturn(assignment);
    when(assigner.assign(block2.id(), snapshot, Parameters.VALIDATOR_THRESHOLD)).thenReturn(assignment);
    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all identifiers
    PublicKey pubKey = mock(PublicKey.class); // mock public key
    Account account = mock(Account.class); // mock account
    when(account.getPublicKey()).thenReturn(pubKey); // returns the mocked public key for all accounts

    // returns true for all signatures
    when(pubKey.verifySignature(any(Block.class), any(Signature.class))).thenReturn(true);
    // returns the mock account for all identifiers
    when(snapshot.getAccount(any(Identifier.class))).thenReturn(account);

    // concurrent block addition
    AtomicInteger threadError = new AtomicInteger();
    ArrayList<Block> concBlockList = new ArrayList<>();
    concBlockList.add(block1);
    concBlockList.add(block2);

    int concurrencyDegree = 2;
    CountDownLatch threadsDone = new CountDownLatch(concurrencyDegree);
    Thread[] threads = new Thread[concurrencyDegree];
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      threads[i] = new Thread(() -> {
        try {
          ingestEngine.process(concBlockList.get(finalI));
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
    Transactions pendingTransactions = mock(Transactions.class);
    Snapshot snapshot = mock(Snapshot.class);
    ValidatorAssigner assigner = mock(ValidatorAssigner.class);
    Identifiers transactionIds = mock(Identifiers.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }
    Block block = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(snapshot.all()).thenReturn(accounts);

    Identifiers seenEntities = mock(Identifiers.class);
    when(seenEntities.has(block.id())).thenReturn(false);

    Blocks blocks = mock(Blocks.class);
    when(blocks.has(block.id())).thenReturn(false);

    for (Transaction tx : block.getTransactions()) {
      when(pendingTransactions.has(tx.id())).thenReturn(false);
    }

    State state = mock(State.class);
    when(state.atBlockId(block.getPreviousBlockId())).thenReturn(snapshot);

    IngestEngine ingestEngine = new IngestEngine(
        state,
        blocks,
        transactionIds,
        pendingTransactions,
        seenEntities,
        assigner);

    // mocks assignment
    Assignment assignment = mock(Assignment.class);
    when(assigner.assign(block.id(), snapshot, Parameters.VALIDATOR_THRESHOLD)).thenReturn(assignment);
    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all identifiers
    PublicKey pubKey = mock(PublicKey.class); // mock public key
    Account account = mock(Account.class); // mock account
    when(account.getPublicKey()).thenReturn(pubKey); // returns the mocked public key for all accounts

    // returns true for all signatures
    when(pubKey.verifySignature(any(Block.class), any(Signature.class))).thenReturn(true);
    // returns the mock account for all identifiers
    when(snapshot.getAccount(any(Identifier.class))).thenReturn(account);

    // action
    ingestEngine.process(block);
    when(seenEntities.has(block.id())).thenReturn(true); // block is already seen
    ingestEngine.process(block);

    // verification
    verifyBlockHappyPathCalled(block, blocks, pendingTransactions, transactionIds, seenEntities);
    verify(seenEntities, times(2)).has(block.id());
  }

  /**
   * Evaluates that when two same validated blocks arrive at ingest engine concurrently (second one should be ignored),
   * the engine adds the blocks to its block storage database.
   * The engine also adds hash of all the transactions of blocks into its "transactions" database.
   */
  @Test
  public void testValidatedSameTwoBlocksConcurrently() {
    Transactions pendingTransactions = mock(Transactions.class);
    Snapshot snapshot = mock(Snapshot.class);
    ValidatorAssigner assigner = mock(ValidatorAssigner.class);
    Identifiers transactionIds = mock(Identifiers.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }
    // first block
    Block block = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(snapshot.all()).thenReturn(accounts);

    Identifiers seenEntities = mock(Identifiers.class);
    when(seenEntities.has(block.id())).thenReturn(false);

    Blocks blocks = mock(Blocks.class);
    when(blocks.has(block.id())).thenReturn(false);

    for (Transaction tx : block.getTransactions()) {
      when(pendingTransactions.has(tx.id())).thenReturn(false);
    }

    State state = mock(State.class);
    when(state.atBlockId(block.getPreviousBlockId())).thenReturn(snapshot);

    IngestEngine ingestEngine = new IngestEngine(
        state,
        blocks,
        transactionIds,
        pendingTransactions,
        seenEntities,
        assigner);

    // mocks assignment
    Assignment assignment = mock(Assignment.class);
    when(assigner.assign(block.id(), snapshot, Parameters.VALIDATOR_THRESHOLD)).thenReturn(assignment);
    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all identifiers
    PublicKey pubKey = mock(PublicKey.class); // mock public key
    Account account = mock(Account.class); // mock account
    when(account.getPublicKey()).thenReturn(pubKey); // returns the mocked public key for all accounts

    // returns true for all signatures
    when(pubKey.verifySignature(any(Block.class), any(Signature.class))).thenReturn(true);
    // returns the mock account for all identifiers
    when(snapshot.getAccount(any(Identifier.class))).thenReturn(account);

    // concurrent block addition
    AtomicInteger threadError = new AtomicInteger();
    int concurrencyDegree = 2;
    CountDownLatch threadsDone = new CountDownLatch(concurrencyDegree);
    Thread[] threads = new Thread[concurrencyDegree];
    for (int i = 0; i < concurrencyDegree; i++) {
      threads[i] = new Thread(() -> {
        try {
          ingestEngine.process(block);
          when(seenEntities.has(block.id())).thenReturn(true); // block is already seen
          when(blocks.has(block.id())).thenReturn(true); // block is already seen
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
  public void testValidatedBlockContainingSeenTransaction() {
    Transactions pendingTransactions = mock(Transactions.class);
    Snapshot snapshot = mock(Snapshot.class);
    ValidatorAssigner assigner = mock(ValidatorAssigner.class);
    Identifiers transactionIds = mock(Identifiers.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }
    Block block = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(snapshot.all()).thenReturn(accounts);

    Identifiers seenEntities = mock(Identifiers.class);
    when(seenEntities.has(block.id())).thenReturn(false);

    Blocks blocks = mock(Blocks.class);
    when(blocks.has(block.id())).thenReturn(false);

    for (Transaction tx : block.getTransactions()) {
      when(pendingTransactions.has(tx.id())).thenReturn(true); // pendingTx contains all txs of block
    }

    State state = mock(State.class);
    when(state.atBlockId(block.getPreviousBlockId())).thenReturn(snapshot);

    IngestEngine ingestEngine = new IngestEngine(
        state,
        blocks,
        transactionIds,
        pendingTransactions,
        seenEntities,
        assigner);

    // mocks assignment
    Assignment assignment = mock(Assignment.class);
    when(assigner.assign(block.id(), snapshot, Parameters.VALIDATOR_THRESHOLD)).thenReturn(assignment);
    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all identifiers
    PublicKey pubKey = mock(PublicKey.class); // mock public key
    Account account = mock(Account.class); // mock account
    when(account.getPublicKey()).thenReturn(pubKey); // returns the mocked public key for all accounts

    // returns true for all signatures
    when(pubKey.verifySignature(any(Block.class), any(Signature.class))).thenReturn(true);
    // returns the mock account for all identifiers
    when(snapshot.getAccount(any(Identifier.class))).thenReturn(account);

    // action
    ingestEngine.process(block);

    // verification
    verifyBlockHappyPathCalled(block, blocks, pendingTransactions, transactionIds, seenEntities);
    for (Transaction tx : block.getTransactions()) {
      verify(pendingTransactions, times(1)).remove(tx.id());
    }
  }

  /**
   * Evaluates that when two new validated blocks (with shared transactions in pendingTx, disjoint set)
   * arrive at ingest engine, the engine adds the blocks to its block storage database.
   * The engine also removes hash of the transactions of blocks from pendingTransactions.
   */
  @Test
  public void testConcurrentBlockIngestionContainingSeenTransactionDisjointSet() {
    Transactions pendingTransactions = mock(Transactions.class);
    Snapshot snapshot = mock(Snapshot.class);
    ValidatorAssigner assigner = mock(ValidatorAssigner.class);
    Identifiers transactionIds = mock(Identifiers.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }
    // first block
    Block block1 = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(snapshot.all()).thenReturn(accounts);

    // second block
    Block block2 = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(snapshot.all()).thenReturn(accounts);

    Identifiers seenEntities = mock(Identifiers.class);
    when(seenEntities.has(block1.id())).thenReturn(false);
    when(seenEntities.has(block2.id())).thenReturn(false);

    Blocks blocks = mock(Blocks.class);
    when(blocks.has(block1.id())).thenReturn(false);
    when(blocks.has(block2.id())).thenReturn(false);

    for (Transaction tx : block1.getTransactions()) {
      when(pendingTransactions.has(tx.id())).thenReturn(false);
    }
    for (Transaction tx : block2.getTransactions()) {
      when(pendingTransactions.has(tx.id())).thenReturn(false);
    }

    State state = mock(State.class);
    when(state.atBlockId(block1.getPreviousBlockId())).thenReturn(snapshot);
    when(state.atBlockId(block2.getPreviousBlockId())).thenReturn(snapshot);

    IngestEngine ingestEngine = new IngestEngine(
        state,
        blocks,
        transactionIds,
        pendingTransactions,
        seenEntities,
        assigner);

    // mocks assignment
    Assignment assignment = mock(Assignment.class);
    when(assigner.assign(block1.id(), snapshot, Parameters.VALIDATOR_THRESHOLD)).thenReturn(assignment);
    when(assigner.assign(block2.id(), snapshot, Parameters.VALIDATOR_THRESHOLD)).thenReturn(assignment);
    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all identifiers
    PublicKey pubKey = mock(PublicKey.class); // mock public key
    Account account = mock(Account.class); // mock account
    when(account.getPublicKey()).thenReturn(pubKey); // returns the mocked public key for all accounts

    // returns true for all signatures
    when(pubKey.verifySignature(any(Block.class), any(Signature.class))).thenReturn(true);
    // returns the mock account for all identifiers
    when(snapshot.getAccount(any(Identifier.class))).thenReturn(account);

    // simulates 1 shared transaction for each block
    when(pendingTransactions.has(block1.getTransactions()[0].id())).thenReturn(true);
    when(pendingTransactions.has(block2.getTransactions()[0].id())).thenReturn(true);

    // action
    AtomicInteger threadError = new AtomicInteger();
    ArrayList<Block> concBlockList = new ArrayList<>();
    concBlockList.add(block1);
    concBlockList.add(block2);

    int concurrencyDegree = 2;
    CountDownLatch threadsDone = new CountDownLatch(concurrencyDegree);
    Thread[] threads = new Thread[concurrencyDegree];
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      threads[i] = new Thread(() -> {
        try {
          ingestEngine.process(concBlockList.get(finalI));
          when(blocks.has(concBlockList.get(finalI).id())).thenReturn(true);
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
    Transactions pendingTransactions = mock(Transactions.class);
    Snapshot snapshot = mock(Snapshot.class);
    ValidatorAssigner assigner = mock(ValidatorAssigner.class);
    Identifiers transactionIds = mock(Identifiers.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }
    // first block
    Block block1 = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(snapshot.all()).thenReturn(accounts);

    // second block
    Block block2 = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(snapshot.all()).thenReturn(accounts);

    Identifiers seenEntities = mock(Identifiers.class);
    when(seenEntities.has(block1.id())).thenReturn(false);
    when(seenEntities.has(block2.id())).thenReturn(false);

    Blocks blocks = mock(Blocks.class);
    when(blocks.has(block1.id())).thenReturn(false);
    when(blocks.has(block2.id())).thenReturn(false);

    for (Transaction tx : block1.getTransactions()) {
      when(pendingTransactions.has(tx.id())).thenReturn(false);
    }
    for (Transaction tx : block2.getTransactions()) {
      when(pendingTransactions.has(tx.id())).thenReturn(false);
    }

    State state = mock(State.class);
    when(state.atBlockId(block1.getPreviousBlockId())).thenReturn(snapshot);
    when(state.atBlockId(block2.getPreviousBlockId())).thenReturn(snapshot);

    IngestEngine ingestEngine = new IngestEngine(
        state,
        blocks,
        transactionIds,
        pendingTransactions,
        seenEntities,
        assigner);

    // mocks assignment
    Assignment assignment = mock(Assignment.class);
    when(assigner.assign(block1.id(), snapshot, Parameters.VALIDATOR_THRESHOLD)).thenReturn(assignment);
    when(assigner.assign(block2.id(), snapshot, Parameters.VALIDATOR_THRESHOLD)).thenReturn(assignment);
    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all identifiers
    PublicKey pubKey = mock(PublicKey.class); // mock public key
    Account account = mock(Account.class); // mock account
    when(account.getPublicKey()).thenReturn(pubKey); // returns the mocked public key for all accounts

    // returns true for all signatures
    when(pubKey.verifySignature(any(Block.class), any(Signature.class))).thenReturn(true);
    // returns the mock account for all identifiers
    when(snapshot.getAccount(any(Identifier.class))).thenReturn(account);

    // simulates an overlapping set of shared transactions
    when(pendingTransactions.has(any(Identifier.class))).thenReturn(true);

    // action
    AtomicInteger threadError = new AtomicInteger();
    ArrayList<Block> concBlockList = new ArrayList<>();
    concBlockList.add(block1);
    concBlockList.add(block2);

    int concurrencyDegree = 2;
    CountDownLatch threadsDone = new CountDownLatch(concurrencyDegree);
    Thread[] threads = new Thread[concurrencyDegree];
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      threads[i] = new Thread(() -> {
        try {
          ingestEngine.process(concBlockList.get(finalI));
          when(blocks.has(concBlockList.get(finalI).id())).thenReturn(true);
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
   * Evaluates that when av already ingested validated block arrives at ingest engine,
   * the engine discards the block right away.
   */
  @Test
  public void testValidatedAlreadyIngestedBlock() {
    Transactions pendingTransactions = mock(Transactions.class);
    Snapshot snapshot = mock(Snapshot.class);
    ValidatorAssigner assigner = mock(ValidatorAssigner.class);
    Identifiers transactionIds = mock(Identifiers.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }
    Block block = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(snapshot.all()).thenReturn(accounts);

    Identifiers seenEntities = mock(Identifiers.class);
    when(seenEntities.has(block.id())).thenReturn(true); // block is already ingested

    Blocks blocks = mock(Blocks.class);
    when(blocks.has(block.id())).thenReturn(true); // block is already ingested

    for (Transaction tx : block.getTransactions()) {
      when(pendingTransactions.has(tx.id())).thenReturn(false);
    }

    State state = mock(State.class);
    when(state.atBlockId(block.getPreviousBlockId())).thenReturn(snapshot);

    IngestEngine ingestEngine = new IngestEngine(
        state,
        blocks,
        transactionIds,
        pendingTransactions,
        seenEntities,
        assigner);

    // mocks assignment
    Assignment assignment = mock(Assignment.class);
    when(assigner.assign(block.id(), snapshot, Parameters.VALIDATOR_THRESHOLD)).thenReturn(assignment);
    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all identifiers
    PublicKey pubKey = mock(PublicKey.class); // mock public key
    Account account = mock(Account.class); // mock account
    when(account.getPublicKey()).thenReturn(pubKey); // returns the mocked public key for all accounts

    // returns true for all signatures
    when(pubKey.verifySignature(any(Block.class), any(Signature.class))).thenReturn(true);
    // returns the mock account for all identifiers
    when(snapshot.getAccount(any(Identifier.class))).thenReturn(account);

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
   * the engine adds hash of the transaction into its "transactions" database.
   */
  @Test
  public void testValidatedTransaction() {
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
        assigner);

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
    verifyTransactionHappyPathCalled(tx, seenEntities, transactionIds, pendingTransactions);
  }

  /**
   * Evaluates that when two validated transactions arrives at ingest engine,
   * the engine adds the hashes of the transactions into its "transactions" database.
   */
  @Test
  public void testValidatedTwoTransactions() {
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);

    ValidatedTransaction tx1 = ValidatedTransactionFixture.newValidatedTransaction();
    ValidatedTransaction tx2 = ValidatedTransactionFixture.newValidatedTransaction();

    IngestEngine ingestEngine = this.mockIngestEngineForTwoValidatedTransactions(
        tx1,
        tx2,
        seenEntities,
        transactionIds,
        pendingTransactions);

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
   * the engine adds the hashes of the transactions into its "transactions" database.
   */
  @Test
  public void testConcurrentValidatedTwoTransactions() {
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);

    ValidatedTransaction tx1 = ValidatedTransactionFixture.newValidatedTransaction();
    ValidatedTransaction tx2 = ValidatedTransactionFixture.newValidatedTransaction();

    IngestEngine ingestEngine = this.mockIngestEngineForTwoValidatedTransactions(
        tx1,
        tx2,
        seenEntities,
        transactionIds,
        pendingTransactions);

    // concurrent block addition
    AtomicInteger threadError = new AtomicInteger();
    ArrayList<Transaction> transactionList = new ArrayList<>();
    transactionList.add(tx1);
    transactionList.add(tx2);

    int concurrencyDegree = 2;
    CountDownLatch threadsDone = new CountDownLatch(concurrencyDegree);
    Thread[] threads = new Thread[concurrencyDegree];
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      threads[i] = new Thread(() -> {
        try {
          ingestEngine.process(transactionList.get(finalI));
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

    // verification of tx1
    verifyTransactionHappyPathCalled(tx1, seenEntities, transactionIds, pendingTransactions);

    // verification of tx2
    verifyTransactionHappyPathCalled(tx2, seenEntities, transactionIds, pendingTransactions);
  }

  private IngestEngine mockIngestEngineForTwoValidatedTransactions(
      ValidatedTransaction tx1,
      ValidatedTransaction tx2,
      Identifiers seenEntities,
      Identifiers transactionIds,
      Transactions pendingTransactions) {

    Blocks blocks = mock(Blocks.class);
    Snapshot snapshot = mock(Snapshot.class);
    ValidatorAssigner assigner = mock(ValidatorAssigner.class);


    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }

    when(snapshot.all()).thenReturn(accounts);


    when(seenEntities.has(tx1.id())).thenReturn(false);
    when(seenEntities.has(tx2.id())).thenReturn(false);

    State state = mock(State.class);
    when(state.atBlockId(tx1.getReferenceBlockId())).thenReturn(snapshot);
    when(state.atBlockId(tx2.getReferenceBlockId())).thenReturn(snapshot);


    when(transactionIds.has(tx1.id())).thenReturn(false);
    when(transactionIds.has(tx2.id())).thenReturn(false);

    IngestEngine ingestEngine = new IngestEngine(
        state,
        blocks,
        transactionIds,
        pendingTransactions,
        seenEntities,
        assigner);

    // mocks assignment
    Assignment assignment = mock(Assignment.class);
    when(assigner.assign(tx1.id(), snapshot, Parameters.VALIDATOR_THRESHOLD)).thenReturn(assignment);
    when(assigner.assign(tx2.id(), snapshot, Parameters.VALIDATOR_THRESHOLD)).thenReturn(assignment);
    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all identifiers
    PublicKey pubKey = mock(PublicKey.class); // mock public key
    Account account = mock(Account.class); // mock account
    when(account.getPublicKey()).thenReturn(pubKey); // returns the mocked public key for all accounts

    // returns true for all signatures
    when(pubKey.verifySignature(any(Transaction.class), any(Signature.class))).thenReturn(true);
    // returns the mock account for all identifiers
    when(snapshot.getAccount(any(Identifier.class))).thenReturn(account);


    return ingestEngine;
  }

  /**
   * Evaluates that when two same validated transactions arrive at ingest engine sequentially,
   * the engine adds the hash of the first transaction into its "transactions" database.
   * The engine also discards the second transaction right away.
   */
  @Test
  public void testValidatedSameTwoTransactions() {
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
        assigner);

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
    when(seenEntities.has(tx.id())).thenReturn(true);
    when(pendingTransactions.has(tx.id())).thenReturn(true);
    ingestEngine.process(tx);

    // verification
    verifyTransactionHappyPathCalled(tx, seenEntities, transactionIds, pendingTransactions);
    verify(seenEntities, times(2)).has(tx.id());
  }

  /**
   * Evaluates that when two same validated transactions arrive at ingest engine concurrently,
   * the engine adds the hash of the first transaction into its "transactions" database.
   * The engine also discards the second transaction right away.
   */
  @Test
  public void testValidatedConcurrentDuplicateTwoTransactions() {
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
        assigner);

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

    // concurrent block addition
    AtomicInteger threadError = new AtomicInteger();

    int concurrencyDegree = 2;
    CountDownLatch threadsDone = new CountDownLatch(concurrencyDegree);
    Thread[] threads = new Thread[concurrencyDegree];
    for (int i = 0; i < concurrencyDegree; i++) {
      threads[i] = new Thread(() -> {
        try {
          ingestEngine.process(tx);
          when(seenEntities.has(tx.id())).thenReturn(true);
          when(pendingTransactions.has(tx.id())).thenReturn(true);
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

    // verification
    verify(seenEntities, times(2)).has(tx.id());
    verify(transactionIds, times(1)).has(tx.id());
    verify(pendingTransactions, times(1)).add(tx);

    Assertions.assertEquals(0, threadError.get());
  }

  /**
   * Evaluates that when a validated transaction which is already in txHash arrives at ingest engine,
   * the engine discards the transaction.
   */
  @Test
  public void testValidatedTransactionAlreadyInTxHash() {
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
    when(transactionIds.has(tx.id())).thenReturn(true);

    IngestEngine ingestEngine = new IngestEngine(
        state,
        blocks,
        transactionIds,
        pendingTransactions,
        seenEntities,
        assigner);

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
    verify(pendingTransactions, times(0)).add(tx);
  }

  /**
   * Evaluates that when a validated transaction which is already in pendingTx arrives at ingest engine,
   * the engine discards the transaction.
   */
  @Test
  public void testValidatedTransactionAlreadyInPendingTx() {
    Blocks blocks = mock(Blocks.class);
    Snapshot snapshot = mock(Snapshot.class);
    ValidatorAssigner assigner = mock(ValidatorAssigner.class);
    Identifiers transactionIds = mock(Identifiers.class);

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

    Transactions pendingTransactions = mock(Transactions.class);
    when(pendingTransactions.has(tx.id())).thenReturn(true);

    IngestEngine ingestEngine = new IngestEngine(
        state,
        blocks,
        transactionIds,
        pendingTransactions,
        seenEntities,
        assigner);

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
    verify(pendingTransactions, times(1)).has(tx.id());
    verify(pendingTransactions, times(0)).add(tx);
  }

  /**
   * Evaluates that when an entity that is neither a validated block nor a validated transaction
   * arrives at ingest engine, the engine throws IllegalArgumentException.
   */
  @Test
  public void testNeitherBlockNorTransaction() {
    Transactions pendingTransactions = mock(Transactions.class);
    ValidatorAssigner assigner = mock(ValidatorAssigner.class);
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
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      ingestEngine.process(e);
    });
  }

  /**
   * Evaluates that when a validated block and a validated transaction arrives at ingest engine concurrently,
   * the engine adds the block to its block storage database.
   * The engine also adds hash of all the transactions of block
   * and the hash of the validated transaction into its "transactions" database.
   */
  @Test
  public void testConcurrentValidatedTransactionAndBlockNonOverlapping() {
    Transactions pendingTransactions = mock(Transactions.class);
    Snapshot snapshot = mock(Snapshot.class);
    ValidatorAssigner assigner = mock(ValidatorAssigner.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }
    ValidatedTransaction validatedTx = ValidatedTransactionFixture.newValidatedTransaction();
    Block block = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(snapshot.all()).thenReturn(accounts);

    Identifiers seenEntities = mock(Identifiers.class);
    when(seenEntities.has(block.id())).thenReturn(false);
    when(seenEntities.has(validatedTx.id())).thenReturn(false);

    Blocks blocks = mock(Blocks.class);
    when(blocks.has(block.id())).thenReturn(false);

    Identifiers transactionIds = mock(Identifiers.class);
    when(transactionIds.has(validatedTx.id())).thenReturn(false);

    for (Transaction tx : block.getTransactions()) {
      when(pendingTransactions.has(tx.id())).thenReturn(false);
    }
    when(pendingTransactions.has(validatedTx.id())).thenReturn(false);

    State state = mock(State.class);
    when(state.atBlockId(block.getPreviousBlockId())).thenReturn(snapshot);
    when(state.atBlockId(validatedTx.getReferenceBlockId())).thenReturn(snapshot);

    IngestEngine ingestEngine = new IngestEngine(
        state,
        blocks,
        transactionIds,
        pendingTransactions,
        seenEntities,
        assigner);

    // mocks assignment
    Assignment assignment = mock(Assignment.class);
    when(assigner.assign(block.id(), snapshot, Parameters.VALIDATOR_THRESHOLD)).thenReturn(assignment);
    when(assigner.assign(validatedTx.id(), snapshot, Parameters.VALIDATOR_THRESHOLD)).thenReturn(assignment);
    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all identifiers
    PublicKey pubKey = mock(PublicKey.class); // mock public key
    Account account = mock(Account.class); // mock account
    when(account.getPublicKey()).thenReturn(pubKey); // returns the mocked public key for all accounts

    // returns true for all signatures
    when(pubKey.verifySignature(any(Block.class), any(Signature.class))).thenReturn(true);
    when(pubKey.verifySignature(any(Transaction.class), any(Signature.class))).thenReturn(true);
    // returns the mock account for all identifiers
    when(snapshot.getAccount(any(Identifier.class))).thenReturn(account);

    AtomicInteger threadError = new AtomicInteger();
    ArrayList<Entity> concList = new ArrayList<>();
    concList.add(block);
    concList.add(validatedTx);
    int concurrencyDegree = 2;
    CountDownLatch threadsDone = new CountDownLatch(concurrencyDegree);
    Thread[] threads = new Thread[concurrencyDegree];
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      threads[i] = new Thread(() -> {
        try {
          ingestEngine.process(concList.get(finalI));
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

    // verification for block
    verifyBlockHappyPathCalled(block, blocks, pendingTransactions, transactionIds, seenEntities);

    // verification for transaction
    verifyTransactionHappyPathCalled(validatedTx, seenEntities, transactionIds, pendingTransactions);

    Assertions.assertEquals(0, threadError.get());
  }

  /**
   * Evaluates that when a validated block and a validated transaction (which the block contains)
   * arrives at ingest engine concurrently, the engine adds the block to its block storage database.
   * The engine also adds hash of all the transactions of block into its "transactions" database.
   */
  @Test
  public void testConcurrentValidatedTransactionAndBlockOverlapping() {
    Transactions pendingTransactions = mock(Transactions.class);
    Snapshot snapshot = mock(Snapshot.class);
    ValidatorAssigner assigner = mock(ValidatorAssigner.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }
    Block block = ValidatedBlockFixture.newValidatedBlock(accounts);
    ValidatedTransaction validatedTx = block.getTransactions()[0]; // the transaction is in the block
    when(snapshot.all()).thenReturn(accounts);

    Identifiers seenEntities = mock(Identifiers.class);
    when(seenEntities.has(block.id())).thenReturn(false);
    when(seenEntities.has(validatedTx.id())).thenReturn(false);

    Blocks blocks = mock(Blocks.class);
    when(blocks.has(block.id())).thenReturn(false);

    Identifiers transactionIds = mock(Identifiers.class);
    when(transactionIds.has(validatedTx.id())).thenReturn(false);

    for (Transaction tx : block.getTransactions()) {
      when(pendingTransactions.has(tx.id())).thenReturn(false);
    }
    when(pendingTransactions.has(validatedTx.id())).thenReturn(false);

    State state = mock(State.class);
    when(state.atBlockId(block.getPreviousBlockId())).thenReturn(snapshot);
    when(state.atBlockId(validatedTx.getReferenceBlockId())).thenReturn(snapshot);

    IngestEngine ingestEngine = new IngestEngine(
        state,
        blocks,
        transactionIds,
        pendingTransactions,
        seenEntities,
        assigner);

    // mocks assignment
    Assignment assignment = mock(Assignment.class);
    when(assigner.assign(block.id(), snapshot, Parameters.VALIDATOR_THRESHOLD)).thenReturn(assignment);
    when(assigner.assign(validatedTx.id(), snapshot, Parameters.VALIDATOR_THRESHOLD)).thenReturn(assignment);
    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all identifiers
    PublicKey pubKey = mock(PublicKey.class); // mock public key
    Account account = mock(Account.class); // mock account
    when(account.getPublicKey()).thenReturn(pubKey); // returns the mocked public key for all accounts

    // returns true for all signatures
    when(pubKey.verifySignature(any(Block.class), any(Signature.class))).thenReturn(true);
    when(pubKey.verifySignature(any(Transaction.class), any(Signature.class))).thenReturn(true);
    // returns the mock account for all identifiers
    when(snapshot.getAccount(any(Identifier.class))).thenReturn(account);

    AtomicInteger threadError = new AtomicInteger();
    ArrayList<Entity> concList = new ArrayList<>();
    concList.add(block);
    concList.add(validatedTx);
    int concurrencyDegree = 2;
    CountDownLatch threadsDone = new CountDownLatch(concurrencyDegree);
    Thread[] threads = new Thread[concurrencyDegree];
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      threads[i] = new Thread(() -> {
        try {
          ingestEngine.process(concList.get(finalI));
          when(transactionIds.has(validatedTx.id())).thenReturn(true);
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

    // verification for block
    verify(blocks, times(1)).add(block);
    verify(seenEntities, times(1)).add(block.id());
    for (Transaction tx : block.getTransactions()) {
      verify(transactionIds, times(1)).add(tx.id());
    }

    // verification for transaction
    verify(seenEntities, times(1)).add(validatedTx.id());
    verify(transactionIds, times(1)).has(validatedTx.id());
    verify(transactionIds, times(1)).add(validatedTx.id());

    Assertions.assertEquals(0, threadError.get());
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
}