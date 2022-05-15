package protocol.engines;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import model.Entity;
import model.lightchain.*;
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

import static org.mockito.Mockito.*;

/**
 * Encapsulates tests for ingest engine implementation.
 */
public class IngestEngineTest {
  private static final Random random = new Random();
  private IngestEngine ingestEngine;
  private ValidatedBlock block1;
  private ValidatedBlock block2;
  private ValidatedTransaction transaction1;
  private ValidatedTransaction transaction2;
  private Blocks blocks;
  private Transactions transactions;

  private Identifiers pendingTransactions;
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

//  @BeforeEach
//  public void setUpMock() {
//    transaction1 = ValidatedTransactionFixture.newValidatedTransaction();
//    transaction2 = ValidatedTransactionFixture.newValidatedTransaction();
//    transactions = mock(Transactions.class);
//    block1 = ValidatedBlockFixture.newValidatedBlock();
//    block2 = ValidatedBlockFixture.newValidatedBlock();
//    blocks = mock(Blocks.class);
//    state = mock(State.class);
//    pendingTransactions = mock(Identifiers.class);
//    Identifiers seenEntities = mock(Identifiers.class);
//    snapshot = mock(Snapshot.class);
//    when(state.atBlockId(block1.getTransactions()[0].getReferenceBlockId())).thenReturn(snapshot);
//    when(state.atBlockId(block2.getTransactions()[0].getReferenceBlockId())).thenReturn(snapshot);
//    when(state.atBlockId(block1.getTransactions()[1].getReferenceBlockId())).thenReturn(snapshot);
//
//    when(state.atBlockId(block1.getTransactions()[2].getReferenceBlockId())).thenReturn(snapshot);
//    when(state.atBlockId(transaction1.getReferenceBlockId())).thenReturn(snapshot);
//    when(state.atBlockId(transaction2.getReferenceBlockId())).thenReturn(snapshot);
//    when(transactions.has(transaction1.id())).thenReturn(true);
//    when(transactions.has(transaction2.id())).thenReturn(true);
//
//    when(pendingTransactions.has(block1.getTransactions()[0].id())).thenReturn(true);
//    when(pendingTransactions.has(block1.getTransactions()[1].id())).thenReturn(false);
//    when(pendingTransactions.has(block2.getTransactions()[0].id())).thenReturn(true);
//
//    when(pendingTransactions.has(block1.getTransactions()[2].id())).thenReturn(false);
//
//    when(state.atBlockId(block1.getPreviousBlockId())).thenReturn(snapshot);
//    when(state.atBlockId(block2.getPreviousBlockId())).thenReturn(snapshot);
//    when(blocks.has(block1.id())).thenReturn(true);
//    when(blocks.has(block2.id())).thenReturn(true);
//    blockList = new ArrayList<>();
//    for (int i = 0; i < 10; i++) {
//      ValidatedBlock block = ValidatedBlockFixture.newValidatedBlock();
//      blockList.add(block);
//      when(state.atBlockId(block.getPreviousBlockId())).thenReturn(snapshot);
//      when(blocks.has(block.id())).thenReturn(true);
//    }
//    when(blocks.all()).thenReturn(blockList);
//    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
//    when(snapshot.all()).thenReturn(accounts);
//    ingestEngine = new IngestEngine(state, blocks, pendingTransactions, transactions, seenEntities);
//    for (Account account : accounts) {
//      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
//    }
//  }

  @Test // 1.
  public void testValidatedSingleBlock() {

    // ingest engine set up

    blocks = mock(Blocks.class);
    state = mock(State.class);
    pendingTransactions = mock(Identifiers.class);
    snapshot = mock(Snapshot.class);
    Identifiers seenEntities = mock(Identifiers.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());

    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }

    when(snapshot.all()).thenReturn(accounts);

    ingestEngine = new IngestEngine(state, blocks, pendingTransactions, transactions, seenEntities);

    // block 1 set up

    block1 = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(state.atBlockId(block1.getPreviousBlockId())).thenReturn(snapshot);

    ingestEngine.process(block1);

    // verification

    verify(blocks, times(1)).add(block1);
    Assertions.assertTrue(blocks.has(block1.id()));
  }

  @Test // 2.
  public void testValidatedTwoBlocks() {

    // ingest engine set up

    blocks = mock(Blocks.class);
    state = mock(State.class);
    pendingTransactions = mock(Identifiers.class);
    Identifiers seenEntities = mock(Identifiers.class);
    snapshot = mock(Snapshot.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());

    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }

    when(snapshot.all()).thenReturn(accounts);


    ingestEngine = new IngestEngine(state, blocks, pendingTransactions, transactions, seenEntities);

    // block 1 set up

    block1 = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(state.atBlockId(block1.getPreviousBlockId())).thenReturn(snapshot);

    // block 2 set up

    block2 = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(state.atBlockId(block2.getPreviousBlockId())).thenReturn(snapshot);

    // action

    ingestEngine.process(block1);
    ingestEngine.process(block2);

    // verification

    verify(blocks, times(1)).add(block1);
    verify(blocks, times(1)).add(block2);

    Assertions.assertTrue(blocks.has(block1.id()));
    Assertions.assertTrue(blocks.has(block2.id()));
  }

  @Test // 3.
  public void testValidatedTwoBlocksConcurrently() {

    // ingest engine set up

    blocks = mock(Blocks.class);
    state = mock(State.class);
    pendingTransactions = mock(Identifiers.class);
    Identifiers seenEntities = mock(Identifiers.class);
    snapshot = mock(Snapshot.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());

    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }

    when(snapshot.all()).thenReturn(accounts);

    ingestEngine = new IngestEngine(state, blocks, pendingTransactions, transactions, seenEntities);

    // block 1 set up

    block1 = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(state.atBlockId(block1.getPreviousBlockId())).thenReturn(snapshot);

    // block 2 set up

    block2 = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(state.atBlockId(block2.getPreviousBlockId())).thenReturn(snapshot);

    // concurrent block addition

    AtomicInteger threadError = new AtomicInteger();
    ArrayList<ValidatedBlock> concBlockList = new ArrayList<>();
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

    // verification

    verify(blocks, times(1)).add(block1);
    verify(blocks, times(1)).add(block2);

    Assertions.assertEquals(0, threadError.get());
    Assertions.assertTrue(blocks.has(block1.id()));
    Assertions.assertTrue(blocks.has(block2.id()));
  }

  @Test // 4.
  public void testValidatedSameTwoBlocks() {


    // ingest engine set up

    blocks = mock(Blocks.class);
    state = mock(State.class);
    pendingTransactions = mock(Identifiers.class);
    Identifiers seenEntities = mock(Identifiers.class);
    snapshot = mock(Snapshot.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());

    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }

    when(snapshot.all()).thenReturn(accounts);

    ingestEngine = new IngestEngine(state, blocks, pendingTransactions, transactions, seenEntities);

    // block 1 set up

    block1 = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(state.atBlockId(block1.getPreviousBlockId())).thenReturn(snapshot);

    // action

    ingestEngine.process(block1);
    Blocks blocks2 = blocks; // might need an improvement depending on the implementation
    ingestEngine.process(block1);

    // verification

    verify(blocks, times(2)).add(block1);
    Assertions.assertTrue(blocks.has(block1.id()));
    Assertions.assertEquals(blocks, blocks2); // checks if block are changed, if not it means second block is not added

  }

  @Test // 5.
  public void testValidatedSameTwoBlocksConcurrently() {

    // ingest engine set up

    blocks = mock(Blocks.class);
    state = mock(State.class);
    pendingTransactions = mock(Identifiers.class);
    Identifiers seenEntities = mock(Identifiers.class);
    snapshot = mock(Snapshot.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());

    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }

    when(snapshot.all()).thenReturn(accounts);

    ingestEngine = new IngestEngine(state, blocks, pendingTransactions, transactions, seenEntities);

    // block 1 set up

    block1 = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(state.atBlockId(block1.getPreviousBlockId())).thenReturn(snapshot);

    // action

    AtomicInteger threadError = new AtomicInteger();
    ArrayList<ValidatedBlock> concBlockList = new ArrayList<>();
    concBlockList.add(block1);
    concBlockList.add(block1);
    int concurrencyDegree = 2;
    CountDownLatch threadsDone = new CountDownLatch(concurrencyDegree);
    Thread[] threads = new Thread[concurrencyDegree];
    Blocks[] blocks2 = new Blocks[1];
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      threads[i] = new Thread(() -> {
        try {
          ingestEngine.process(concBlockList.get(finalI));
          if (finalI == 0) {
            blocks2[0] = blocks; // might need an improvement depending on the implementation
          }
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

    verify(blocks, times(2)).add(block1);
    Assertions.assertTrue(blocks.has(block1.id()));
    Assertions.assertEquals(blocks, blocks2); // checks if block are changed, if not it means second block is not added
    Assertions.assertEquals(0, threadError.get());

  }

  @Test // 6.
  public void testValidatedBlockContainingSeenTransaction() {

    // ingest engine set up

    blocks = mock(Blocks.class);
    state = mock(State.class);
    pendingTransactions = mock(Identifiers.class);
    Identifiers seenEntities = mock(Identifiers.class);
    snapshot = mock(Snapshot.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());

    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }

    when(snapshot.all()).thenReturn(accounts);

    ingestEngine = new IngestEngine(state, blocks, pendingTransactions, transactions, seenEntities);

    // block 1 set up

    block1 = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(state.atBlockId(block1.getPreviousBlockId())).thenReturn(snapshot);

    // action

    ingestEngine.process(block1.getTransactions()[2]);
    ingestEngine.process(block1);

    // verification

    verify(blocks, times(1)).add(block1);
    verify(pendingTransactions, times(1)).add(block1.getTransactions()[2].id());
    verify(pendingTransactions, times(1)).remove(block1.getTransactions()[2].id());

  }

  @Test // 7.
  public void testConcurrentBlockIngestionContainingSeenTransactionDisjointSet() {

    // ingest engine set up

    blocks = mock(Blocks.class);
    state = mock(State.class);
    pendingTransactions = mock(Identifiers.class);
    Identifiers seenEntities = mock(Identifiers.class);
    snapshot = mock(Snapshot.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());

    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }

    when(snapshot.all()).thenReturn(accounts);

    ingestEngine = new IngestEngine(state, blocks, pendingTransactions, transactions, seenEntities);

    // block 1 set up

    block1 = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(state.atBlockId(block1.getPreviousBlockId())).thenReturn(snapshot);

    // block 2 set up

    block2 = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(state.atBlockId(block2.getPreviousBlockId())).thenReturn(snapshot);

    // action

    ingestEngine.process(block1.getTransactions()[0]);
    ingestEngine.process(block2.getTransactions()[0]);

    AtomicInteger threadError = new AtomicInteger();
    ArrayList<ValidatedBlock> concBlockList = new ArrayList<>();
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

    // verification

    verify(blocks, times(1)).add(block1);
    verify(blocks, times(1)).add(block2);

    verify(pendingTransactions, times(1)).add(block1.getTransactions()[0].id());
    verify(pendingTransactions, times(1)).remove(block1.getTransactions()[0].id());


    verify(pendingTransactions, times(1)).add(block2.getTransactions()[0].id());
    verify(pendingTransactions, times(1)).remove(block2.getTransactions()[0].id());

    Assertions.assertEquals(0, threadError.get());

  }

  @Test // 8.
  public void testConcurrentBlockIngestionContainingSeenTransactionOverlappingSet() {

    // ingest engine set up

    blocks = mock(Blocks.class);
    state = mock(State.class);
    pendingTransactions = mock(Identifiers.class);
    Identifiers seenEntities = mock(Identifiers.class);
    snapshot = mock(Snapshot.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());

    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }

    when(snapshot.all()).thenReturn(accounts);

    ingestEngine = new IngestEngine(state, blocks, pendingTransactions, transactions, seenEntities);

    // block 1 set up

    block1 = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(state.atBlockId(block1.getPreviousBlockId())).thenReturn(snapshot);

    // temp block set up

    ValidatedTransaction[] tempTransactions = new ValidatedTransaction[block1.getTransactions().length];

    for (int i = 0; i < block1.getTransactions().length; i++) {
      if (i == 0) {
        int senderIndex = random.nextInt(accounts.size());
        int receiverIndex = random.nextInt(accounts.size());
        while (receiverIndex == senderIndex) {
          receiverIndex = random.nextInt(accounts.size());
        }
        tempTransactions[i] = ValidatedTransactionFixture.newValidatedTransaction(
                accounts.get(senderIndex).getIdentifier(),
                accounts.get(receiverIndex).getIdentifier());
      } else {
        tempTransactions[i] = block1.getTransactions()[i];
      }
    }

    ValidatedBlock tempBlock = new ValidatedBlock(block1.getPreviousBlockId(), block1.getProposer(),
            tempTransactions, block1.getSignature(), block1.getCertificates(), block1.getHeight());

    when(state.atBlockId(tempBlock.getPreviousBlockId())).thenReturn(snapshot);

    // action

    ingestEngine.process(block1.getTransactions()[0]);

    AtomicInteger threadError = new AtomicInteger();
    ArrayList<ValidatedBlock> concBlockList = new ArrayList<>();
    concBlockList.add(block1);
    concBlockList.add(tempBlock);

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

    // verification

    verify(blocks, times(1)).add(block1);
    verify(blocks, times(1)).add(tempBlock);

    verify(transactionList, times(2)).add(block1.getTransactions()[0]);

    verify(pendingTransactions, times(1)).add(block1.getTransactions()[0].id());
    verify(pendingTransactions, times(1)).remove(block1.getTransactions()[0].id());

    Assertions.assertEquals(0, threadError.get());

  }

  @Test // 9.
  public void testValidatedAlreadyIngestedBlock() {

    // ingest engine set up

    blocks = mock(Blocks.class);
    state = mock(State.class);
    pendingTransactions = mock(Identifiers.class);
    Identifiers seenEntities = mock(Identifiers.class);
    snapshot = mock(Snapshot.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());

    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }

    when(snapshot.all()).thenReturn(accounts);


    ingestEngine = new IngestEngine(state, blocks, pendingTransactions, transactions, seenEntities);

    // block 1 set up

    block1 = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(state.atBlockId(block1.getPreviousBlockId())).thenReturn(snapshot);

    // action

    ingestEngine.process(block1);
    ingestEngine.process(block1);

    // verification

    verify(blocks, times(1)).add(block1);

  }

  @Test // 10.
  public void testValidatedTransaction() {

    // ingest engine set up

    transactions = mock(Transactions.class);
    blocks = mock(Blocks.class);
    state = mock(State.class);
    pendingTransactions = mock(Identifiers.class);
    Identifiers seenEntities = mock(Identifiers.class);
    snapshot = mock(Snapshot.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());

    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }

    when(snapshot.all()).thenReturn(accounts);

    ingestEngine = new IngestEngine(state, blocks, pendingTransactions, transactions, seenEntities);

    // transaction 1 set up

    transaction1 = ValidatedTransactionFixture.newValidatedTransaction();

    // action

    ingestEngine.process(transaction1);

    // validation

    verify(pendingTransactions, times(1)).add(transaction1.id());
    Assertions.assertTrue(pendingTransactions.has(transaction1.id()));

  }

  @Test // 11.
  public void testValidatedTwoTransactions() {

    // ingest engine set up

    transactions = mock(Transactions.class);
    blocks = mock(Blocks.class);
    state = mock(State.class);
    pendingTransactions = mock(Identifiers.class);
    Identifiers seenEntities = mock(Identifiers.class);
    snapshot = mock(Snapshot.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());

    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }

    when(snapshot.all()).thenReturn(accounts);

    ingestEngine = new IngestEngine(state, blocks, pendingTransactions, transactions, seenEntities);

    // transaction 1 set up

    transaction1 = ValidatedTransactionFixture.newValidatedTransaction();

    // transaction 2 set up

    transaction2 = ValidatedTransactionFixture.newValidatedTransaction();

    // action

    ingestEngine.process(transaction1);
    ingestEngine.process(transaction2);

    // validation

    verify(pendingTransactions, times(1)).add(transaction1.id());
    verify(pendingTransactions, times(1)).add(transaction2.id());
    Assertions.assertTrue(pendingTransactions.has(transaction1.id()));
    Assertions.assertTrue(pendingTransactions.has(transaction2.id()));

  }

  @Test // 12.
  public void testConcurrentValidatedTwoTransactions() {

    AtomicInteger threadError = new AtomicInteger();
    ArrayList<ValidatedTransaction> concTrxList = new ArrayList<>();
    concTrxList.add(transaction1);
    concTrxList.add(transaction2);
    int concurrencyDegree = 2;
    CountDownLatch threadsDone = new CountDownLatch(concurrencyDegree);
    Thread[] threads = new Thread[concurrencyDegree];
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      threads[i] = new Thread(() -> {
        try {
          ingestEngine.process(concTrxList.get(finalI));
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

  @Test // 14.
  public void testValidatedConcurrentDuplicateTwoTransactions() {

    AtomicInteger threadError = new AtomicInteger();
    ArrayList<ValidatedTransaction> concTrxList = new ArrayList<>();
    concTrxList.add(transaction1);
    concTrxList.add(transaction1);
    int concurrencyDegree = 2;
    CountDownLatch threadsDone = new CountDownLatch(concurrencyDegree);
    Thread[] threads = new Thread[concurrencyDegree];
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      threads[i] = new Thread(() -> {
        try {
          ingestEngine.process(concTrxList.get(finalI));
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

    Assertions.assertTrue(transactions.has(transaction1.id()));

  }

  @Test // 15.
  public void testValidatedTransactionAlreadyInTxHash() {
    ingestEngine.process(transaction1);
    ingestEngine.process(transaction1);
    Assertions.assertTrue(transactions.has(transaction1.id()));
  }

  @Test // 16.
  public void testValidatedTransactionAlreadyInPendingTx() {
    ingestEngine.process(transaction1);
    ingestEngine.process(transaction1);
    Assertions.assertTrue(transactions.has(transaction1.id()));
  }

  @Test // 17.
  public void testNeitherBlockNorTransaction() {
    Entity e = new EntityFixture(); // not a block nor a transaction
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      ingestEngine.process(e);
    });
  }

  @Test // 18.
  public void testConcurrentValidatedTransactionAndBlockNonOverlapping() {

    AtomicInteger threadError = new AtomicInteger();
    ArrayList<Object> concList = new ArrayList<>();

    concList.add(block1);
    concList.add(block2.getTransactions()[0]);

    int concurrencyDegree = 2;
    CountDownLatch threadsDone = new CountDownLatch(concurrencyDegree);
    Thread[] threads = new Thread[concurrencyDegree];
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      threads[i] = new Thread(() -> {
        try {
          ingestEngine.process((Entity) concList.get(finalI));
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
    Assertions.assertTrue(pendingTransactions.has(block1.getTransactions()[0].id()));
    Assertions.assertTrue(pendingTransactions.has(block2.getTransactions()[0].id()));

  }

  @Test // 19.
  public void testConcurrentValidatedTransactionAndBlockOverlapping() {

    AtomicInteger threadError = new AtomicInteger();
    ArrayList<Object> concList = new ArrayList<>();

    concList.add(block1);
    concList.add(block1.getTransactions()[1]);

    int concurrencyDegree = 2;
    CountDownLatch threadsDone = new CountDownLatch(concurrencyDegree);
    Thread[] threads = new Thread[concurrencyDegree];
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      threads[i] = new Thread(() -> {
        try {
          ingestEngine.process((Entity) concList.get(finalI));
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
    Assertions.assertTrue(pendingTransactions.has(block1.getTransactions()[0].id()));
    Assertions.assertTrue(!pendingTransactions.has(block1.getTransactions()[1].id()));

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
