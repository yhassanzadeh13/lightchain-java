package protocol.engines;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import model.Entity;
import model.lightchain.Account;
import model.lightchain.Block;
import model.lightchain.Transaction;
import model.lightchain.ValidatedBlock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import state.Snapshot;
import state.State;
import storage.Blocks;
import storage.Identifiers;
import storage.Transactions;
import unittest.fixtures.AccountFixture;
import unittest.fixtures.EntityFixture;
import unittest.fixtures.ValidatedBlockFixture;
import unittest.fixtures.ValidatedTransactionFixture;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Encapsulates tests for ingest engine implementation.
 */
public class IngestEngineTest {
  private IngestEngine ingestEngine;
  private ValidatedBlock block1;
  private ValidatedBlock block2;
  private Transaction transaction1;
  private Transaction transaction2;
  private Blocks blocks;
  private Transactions transactions;
  private Snapshot snapshot;
  private State state;
  private ArrayList<Block> blockList;
  private ArrayList<Transaction> transactionList;
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

  @BeforeEach
  public void setUpMock() {
    transaction1 = ValidatedTransactionFixture.newValidatedTransaction();
    transaction2 = ValidatedTransactionFixture.newValidatedTransaction();
    transactions = mock(Transactions.class);
    block1 = ValidatedBlockFixture.newValidatedBlock();
    block2 = ValidatedBlockFixture.newValidatedBlock();
    blocks = mock(Blocks.class);
    state = mock(State.class);
    Identifiers pendingTransactions = mock(Identifiers.class);
    Identifiers seenEntities = mock(Identifiers.class);
    snapshot = mock(Snapshot.class);
    when(state.atBlockId(transaction1.getReferenceBlockId())).thenReturn(snapshot);
    when(state.atBlockId(transaction2.getReferenceBlockId())).thenReturn(snapshot);
    when(transactions.has(transaction1.id())).thenReturn(true);
    when(transactions.has(transaction2.id())).thenReturn(true);
    when(state.atBlockId(block1.getPreviousBlockId())).thenReturn(snapshot);
    when(state.atBlockId(block2.getPreviousBlockId())).thenReturn(snapshot);
    when(blocks.has(block1.id())).thenReturn(true);
    when(blocks.has(block2.id())).thenReturn(true);
    blockList = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      ValidatedBlock block = ValidatedBlockFixture.newValidatedBlock();
      blockList.add(block);
      when(state.atBlockId(block.getPreviousBlockId())).thenReturn(snapshot);
      when(blocks.has(block.id())).thenReturn(true);
    }
    when(blocks.all()).thenReturn(blockList);
    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    when(snapshot.all()).thenReturn(accounts);
    ingestEngine = new IngestEngine(state, blocks, pendingTransactions, transactions, seenEntities);
    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }
  }

  @Test // 1.
  public void testValidatedSingleBlock() {
    ingestEngine.process(block1);
    Assertions.assertTrue(blocks.has(block1.id()));
  }

  @Test // 2.
  public void testValidatedTwoBlocks() {
    ingestEngine.process(block1);
    ingestEngine.process(block2);
    Assertions.assertTrue(blocks.has(block1.id()));
    Assertions.assertTrue(blocks.has(block2.id()));
  }

  @Test // 4.
  public void testValidatedSameTwoBlocks() {
    ingestEngine.process(block1);
    Blocks blocks2 = blocks; // might need an improvement depending on the implementation
    ingestEngine.process(block1);
    Assertions.assertEquals(blocks, blocks2); // checks if block are changed, if not it means second block is not added
  }

  @Test // 9.
  public void testValidatedAlreadyIngestedBlock() {
    ArrayList<Block> blocksInDatabase = this.blocks.all();
    Blocks blocks2 = blocks; // might need an improvement depending on the implementation
    ingestEngine.process(blocksInDatabase.get(0)); // gets an already ingested block
    Assertions.assertEquals(blocks, blocks2); // checks if block are changed, if not it means second block is not added
  }

  @Test // 10.
  public void testValidatedTransaction() {
    ingestEngine.process(transaction1);
    Assertions.assertTrue(transactions.has(transaction1.id()));
  }

  @Test // 11.
  public void testValidatedTwoTransactions() {
    ingestEngine.process(transaction1);
    ingestEngine.process(transaction2);
    Assertions.assertTrue(transactions.has(transaction1.id()));
    Assertions.assertTrue(transactions.has(transaction2.id()));
  }

  @Test // 13.
  public void testValidatedSameTwoTransactions() {
    ingestEngine.process(transaction1);
    Transactions transactions2 = transactions; // might need an improvement depending on the implementation
    ingestEngine.process(transaction2);
    Assertions.assertEquals(transactions, transactions2); // checks if transactions are changed,
    // if not it means second transaction is not added
  }

  @Test // 17.
  public void testNeitherBlockNorTransaction() {
    Entity e = new EntityFixture(); // not a block nor a transaction
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      ingestEngine.process(e);
    });
  }

  @Test
  public void testConcurrentSample() {
    AtomicInteger threadErrorCount = new AtomicInteger();
    CountDownLatch done = new CountDownLatch(1);

    Thread t = new Thread(() -> {
      // implement body of thread.
      // if some error happens that leads to test failure:
      // threadErrorCount.getAndIncrement();

      done.countDown();
    });

    // run threads
    t.start();

    try {
      boolean doneOnTime = done.await(1, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOnTime);
    } catch (InterruptedException e) {
      Assertions.fail(e);
    }

    Assertions.assertEquals(0, threadErrorCount.get());
  }
}
