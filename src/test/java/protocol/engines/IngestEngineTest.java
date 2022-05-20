package protocol.engines;

import java.util.ArrayList;
import java.util.Random;
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

  @Test // 1.
  public void testValidatedSingleBlock() {

    // ingest engine set up
    blocks = mock(Blocks.class);
    state = mock(State.class);
    pendingTransactions = mock(Transactions.class);
    snapshot = mock(Snapshot.class);
    assignment = mock(Assignment.class);
    transactionIds = mock(Identifiers.class);
    seenEntities = mock(Identifiers.class);
    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }
    when(snapshot.all()).thenReturn(accounts);
    ingestEngine = new IngestEngine(state, blocks, transactionIds, pendingTransactions, seenEntities, assignment);

    // block 1 set up
    block1 = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(state.atBlockId(block1.getPreviousBlockId())).thenReturn(snapshot);

    // assignment mock
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

    // verification
    verify(blocks, times(1)).add(block1);
  }

  @Test // 2.
  public void testValidatedTwoBlocks() {

    // ingest engine set up
    blocks = mock(Blocks.class);
    state = mock(State.class);
    pendingTransactions = mock(Transactions.class);
    snapshot = mock(Snapshot.class);
    assignment = mock(Assignment.class);
    transactionIds = mock(Identifiers.class);
    seenEntities = mock(Identifiers.class);
    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }
    when(snapshot.all()).thenReturn(accounts);
    ingestEngine = new IngestEngine(state, blocks, transactionIds, pendingTransactions, seenEntities, assignment);

    // block 1 set up
    block1 = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(state.atBlockId(block1.getPreviousBlockId())).thenReturn(snapshot);

    // block 2 set up
    block2 = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(state.atBlockId(block2.getPreviousBlockId())).thenReturn(snapshot);

    // assignment mock
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

    // verification
    verify(blocks, times(1)).add(block1);
    verify(blocks, times(1)).add(block2);
  }

  @Test // 3.
  public void testValidatedTwoBlocksConcurrently() {

    // ingest engine set up
    blocks = mock(Blocks.class);
    state = mock(State.class);
    pendingTransactions = mock(Transactions.class);
    snapshot = mock(Snapshot.class);
    assignment = mock(Assignment.class);
    transactionIds = mock(Identifiers.class);
    seenEntities = mock(Identifiers.class);
    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }
    when(snapshot.all()).thenReturn(accounts);
    ingestEngine = new IngestEngine(state, blocks, transactionIds, pendingTransactions, seenEntities, assignment);

    // assignment mock
    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all identifiers
    PublicKey pubKey = mock(PublicKey.class); // mock public key
    Account account = mock(Account.class); // mock account
    when(account.getPublicKey()).thenReturn(pubKey); // returns the mocked public key for all accounts
    // returns true for all signatures
    when(pubKey.verifySignature(any(Block.class), any(Signature.class))).thenReturn(true);
    // returns the mock account for all identifiers
    when(snapshot.getAccount(any(Identifier.class))).thenReturn(account);

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

  }

  @Test // 4.
  public void testValidatedSameTwoBlocks() {

    // ingest engine set up
    blocks = mock(Blocks.class);
    state = mock(State.class);
    pendingTransactions = mock(Transactions.class);
    snapshot = mock(Snapshot.class);
    assignment = mock(Assignment.class);
    transactionIds = mock(Identifiers.class);
    seenEntities = mock(Identifiers.class);
    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }
    when(snapshot.all()).thenReturn(accounts);
    ingestEngine = new IngestEngine(state, blocks, transactionIds, pendingTransactions, seenEntities, assignment);

    // block 1 set up
    block1 = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(state.atBlockId(block1.getPreviousBlockId())).thenReturn(snapshot);

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
    when(blocks.has(block1.id())).thenReturn(true);
    ingestEngine.process(block1);

    // verification
    verify(blocks, times(1)).add(block1);
  }

  @Test // 5.
  public void testValidatedSameTwoBlocksConcurrently() {

    // ingest engine set up
    blocks = mock(Blocks.class);
    state = mock(State.class);
    pendingTransactions = mock(Transactions.class);
    snapshot = mock(Snapshot.class);
    assignment = mock(Assignment.class);
    transactionIds = mock(Identifiers.class);
    seenEntities = mock(Identifiers.class);
    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }
    when(snapshot.all()).thenReturn(accounts);
    ingestEngine = new IngestEngine(state, blocks, transactionIds, pendingTransactions, seenEntities, assignment);

    // assignment mock
    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all identifiers
    PublicKey pubKey = mock(PublicKey.class); // mock public key
    Account account = mock(Account.class); // mock account
    when(account.getPublicKey()).thenReturn(pubKey); // returns the mocked public key for all accounts
    // returns true for all signatures
    when(pubKey.verifySignature(any(Block.class), any(Signature.class))).thenReturn(true);
    // returns the mock account for all identifiers
    when(snapshot.getAccount(any(Identifier.class))).thenReturn(account);

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
          when(seenEntities.has(block1.id())).thenReturn(true);
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

    verify(blocks, times(1)).add(block1);
    Assertions.assertEquals(0, threadError.get());

  }

  @Test // 6.
  public void testValidatedBlockContainingSeenTransaction() {

    // ingest engine set up
    blocks = mock(Blocks.class);
    state = mock(State.class);
    pendingTransactions = mock(Transactions.class);
    snapshot = mock(Snapshot.class);
    assignment = mock(Assignment.class);
    transactionIds = mock(Identifiers.class);
    seenEntities = mock(Identifiers.class);
    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }
    when(snapshot.all()).thenReturn(accounts);
    ingestEngine = new IngestEngine(state, blocks, transactionIds, pendingTransactions, seenEntities, assignment);

    // block 1 set up
    block1 = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(state.atBlockId(block1.getPreviousBlockId())).thenReturn(snapshot);
    when(state.atBlockId(block1.getTransactions()[0].getReferenceBlockId())).thenReturn(snapshot);
    when(transactionIds.has(any(Identifier.class))).thenReturn(true);
    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all identifiers
    PublicKey pubKey = mock(PublicKey.class); // mock public key
    Account account = mock(Account.class); // mock account
    when(account.getPublicKey()).thenReturn(pubKey); // returns the mocked public key for all accounts
    // returns true for all signatures
    when(pubKey.verifySignature(any(Block.class), any(Signature.class))).thenReturn(true);
    // returns the mock account for all identifiers
    when(snapshot.getAccount(any(Identifier.class))).thenReturn(account);
    // simulates shared transactions (all transactions are shared)
    when(pendingTransactions.has(any(Identifier.class))).thenReturn(true);
    // action

    ingestEngine.process(block1.getTransactions()[0]);
    ingestEngine.process(block1);

    // verification
    verify(blocks, times(1)).add(block1);
    // Parameters.MIN_TRANSACTIONS_NUM + 2 transactions in the block as ValidatedBlockFixture dictates
    verify(pendingTransactions, times(Parameters.MIN_TRANSACTIONS_NUM + 2)).remove(any(Identifier.class));
    verify(transactionIds, times(Parameters.MIN_TRANSACTIONS_NUM + 2)).add(any(Identifier.class));

  }

  @Test // 7.
  public void testConcurrentBlockIngestionContainingSeenTransactionDisjointSet() {

    // ingest engine set up
    blocks = mock(Blocks.class);
    state = mock(State.class);
    pendingTransactions = mock(Transactions.class);
    snapshot = mock(Snapshot.class);
    assignment = mock(Assignment.class);
    transactionIds = mock(Identifiers.class);
    seenEntities = mock(Identifiers.class);
    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }
    when(snapshot.all()).thenReturn(accounts);
    ingestEngine = new IngestEngine(state, blocks, transactionIds, pendingTransactions, seenEntities, assignment);

    // assignment mock
    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all identifiers
    PublicKey pubKey = mock(PublicKey.class); // mock public key
    Account account = mock(Account.class); // mock account
    when(account.getPublicKey()).thenReturn(pubKey); // returns the mocked public key for all accounts
    // returns true for all signatures
    when(pubKey.verifySignature(any(Block.class), any(Signature.class))).thenReturn(true);
    // returns the mock account for all identifiers
    when(snapshot.getAccount(any(Identifier.class))).thenReturn(account);

    // block 1 set up
    block1 = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(state.atBlockId(block1.getPreviousBlockId())).thenReturn(snapshot);

    // block 2 set up
    block2 = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(state.atBlockId(block2.getPreviousBlockId())).thenReturn(snapshot);

    // simulates 1 shared transaction for each block
    when(pendingTransactions.has(block1.getTransactions()[0].id())).thenReturn(true);
    when(pendingTransactions.has(block2.getTransactions()[0].id())).thenReturn(true);

    // action
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

    // verification
    verify(blocks, times(1)).add(block1);
    verify(blocks, times(1)).add(block2);

    // verify that shared transactions are removed from pending transactions
    verify(pendingTransactions, times(1)).remove(block1.getTransactions()[0].id());
    verify(pendingTransactions, times(1)).remove(block2.getTransactions()[0].id());
    // verify that non-shared transactions are added to seen entities
    verify(transactionIds, times(1)).add(block1.getTransactions()[1].id());
    verify(transactionIds, times(1)).add(block1.getTransactions()[2].id());
    verify(transactionIds, times(1)).add(block2.getTransactions()[1].id());
    verify(transactionIds, times(1)).add(block2.getTransactions()[2].id());

    Assertions.assertEquals(0, threadError.get());
  }

  @Test // 8.
  public void testConcurrentBlockIngestionContainingSeenTransactionOverlappingSet() {

    // ingest engine set up
    blocks = mock(Blocks.class);
    state = mock(State.class);
    pendingTransactions = mock(Transactions.class);
    snapshot = mock(Snapshot.class);
    assignment = mock(Assignment.class);
    transactionIds = mock(Identifiers.class);
    seenEntities = mock(Identifiers.class);
    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }
    when(snapshot.all()).thenReturn(accounts);
    ingestEngine = new IngestEngine(state, blocks, transactionIds, pendingTransactions, seenEntities, assignment);

    // assignment mock
    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all identifiers
    PublicKey pubKey = mock(PublicKey.class); // mock public key
    Account account = mock(Account.class); // mock account
    when(account.getPublicKey()).thenReturn(pubKey); // returns the mocked public key for all accounts
    // returns true for all signatures
    when(pubKey.verifySignature(any(Block.class), any(Signature.class))).thenReturn(true);
    // returns the mock account for all identifiers
    when(snapshot.getAccount(any(Identifier.class))).thenReturn(account);

    // block 1 set up
    block1 = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(state.atBlockId(block1.getPreviousBlockId())).thenReturn(snapshot);

    // block 2 set up
    block2 = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(state.atBlockId(block2.getPreviousBlockId())).thenReturn(snapshot);

    // simulates a shared set transaction for each block
    when(pendingTransactions.has(any(Identifier.class))).thenReturn(true);

    // action
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

    // verification
    verify(blocks, times(1)).add(block1);
    verify(blocks, times(1)).add(block2);

    // verify that shared transactions are removed from pending transactions
    for (int i = 0; i < block1.getTransactions().length; i++) {
      verify(pendingTransactions, times(1)).remove(block1.getTransactions()[i].id());
      verify(pendingTransactions, times(1)).remove(block2.getTransactions()[i].id());
    }
    Assertions.assertEquals(0, threadError.get());
  }

  @Test // 9.
  public void testValidatedAlreadyIngestedBlock() {

    // ingest engine set up
    blocks = mock(Blocks.class);
    state = mock(State.class);
    pendingTransactions = mock(Transactions.class);
    snapshot = mock(Snapshot.class);
    assignment = mock(Assignment.class);
    transactionIds = mock(Identifiers.class);
    seenEntities = mock(Identifiers.class);
    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    for (Account account : accounts) {
      when(snapshot.getAccount(account.getIdentifier())).thenReturn(account);
    }
    when(snapshot.all()).thenReturn(accounts);
    ingestEngine = new IngestEngine(state, blocks, transactionIds, pendingTransactions, seenEntities, assignment);

    // block 1 set up
    block1 = ValidatedBlockFixture.newValidatedBlock(accounts);
    when(state.atBlockId(block1.getPreviousBlockId())).thenReturn(snapshot);

    // assignment mock
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
    when(seenEntities.has(block1.id())).thenReturn(true); // block 1 already added to blocks database
    ingestEngine.process(block1);
    // verification
    verify(blocks, times(1)).add(block1);

  }

  @Test // 10.
  public void testValidatedTransaction() {
    // ingest engine set up
    blocks = mock(Blocks.class);
    state = mock(State.class);
    pendingTransactions = mock(Transactions.class);
    snapshot = mock(Snapshot.class);
    assignment = mock(Assignment.class);
    transactionIds = mock(Identifiers.class);
    seenEntities = mock(Identifiers.class);
    ValidatedTransaction validatedTransaction = ValidatedTransactionFixture.newValidatedTransaction();

    when(state.atBlockId(validatedTransaction.getReferenceBlockId())).thenReturn(snapshot);
    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all certificates
    PublicKey pubKey = mock(PublicKey.class); // mock public key
    Account account = mock(Account.class); // mock account
    when(account.getPublicKey()).thenReturn(pubKey); // returns the mocked public key for all accounts
    // returns true for all signatures
    when(pubKey.verifySignature(any(Transaction.class), any(Signature.class))).thenReturn(true);
    for (Signature certificate : validatedTransaction.getCertificates()) {
      when(snapshot.getAccount(certificate.getSignerId())).thenReturn(account);
    }
    ingestEngine = new IngestEngine(state, blocks, transactionIds, pendingTransactions, seenEntities, assignment);
    ingestEngine.process(validatedTransaction);
    verify(pendingTransactions, times(1)).add(validatedTransaction);
  }

  // 11. Happy path of receiving two validated transactions sequentially.
  @Test // 11.
  public void testValidatedTwoTransactions() {
    // ingest engine set up
    blocks = mock(Blocks.class);
    state = mock(State.class);
    pendingTransactions = mock(Transactions.class);
    snapshot = mock(Snapshot.class);
    assignment = mock(Assignment.class);
    transactionIds = mock(Identifiers.class);
    seenEntities = mock(Identifiers.class);
    ValidatedTransaction validatedTransaction1 = ValidatedTransactionFixture.newValidatedTransaction();
    ValidatedTransaction validatedTransaction2 = ValidatedTransactionFixture.newValidatedTransaction();

    when(state.atBlockId(validatedTransaction1.getReferenceBlockId())).thenReturn(snapshot);
    when(state.atBlockId(validatedTransaction2.getReferenceBlockId())).thenReturn(snapshot);
    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all certificates
    PublicKey pubKey = mock(PublicKey.class); // mock public key
    Account account = mock(Account.class); // mock account
    when(account.getPublicKey()).thenReturn(pubKey); // returns the mocked public key for all accounts
    // returns true for all signatures
    when(pubKey.verifySignature(any(Transaction.class), any(Signature.class))).thenReturn(true);
    for (Signature certificate : validatedTransaction1.getCertificates()) {
      when(snapshot.getAccount(certificate.getSignerId())).thenReturn(account);
    }
    for (Signature certificate : validatedTransaction2.getCertificates()) {
      when(snapshot.getAccount(certificate.getSignerId())).thenReturn(account);
    }
    ingestEngine = new IngestEngine(state, blocks, transactionIds, pendingTransactions, seenEntities, assignment);
    ingestEngine.process(validatedTransaction1);
    ingestEngine.process(validatedTransaction2);
    verify(pendingTransactions, times(1)).add(validatedTransaction1);
    verify(pendingTransactions, times(1)).add(validatedTransaction2);

  }

  // 12. Happy path of receiving two validated transactions concurrently.
  @Test // 12.
  public void testConcurrentValidatedTwoTransactions() {
    // ingest engine set up
    blocks = mock(Blocks.class);
    state = mock(State.class);
    pendingTransactions = mock(Transactions.class);
    snapshot = mock(Snapshot.class);
    assignment = mock(Assignment.class);
    transactionIds = mock(Identifiers.class);
    seenEntities = mock(Identifiers.class);
    ValidatedTransaction validatedTransaction1 = ValidatedTransactionFixture.newValidatedTransaction();
    ValidatedTransaction validatedTransaction2 = ValidatedTransactionFixture.newValidatedTransaction();
    when(state.atBlockId(validatedTransaction1.getReferenceBlockId())).thenReturn(snapshot);
    when(state.atBlockId(validatedTransaction2.getReferenceBlockId())).thenReturn(snapshot);
    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all certificates
    PublicKey pubKey = mock(PublicKey.class); // mock public key
    Account account = mock(Account.class); // mock account
    when(account.getPublicKey()).thenReturn(pubKey); // returns the mocked public key for all accounts
    // returns true for all signatures
    when(pubKey.verifySignature(any(Transaction.class), any(Signature.class))).thenReturn(true);
    for (Signature certificate : validatedTransaction1.getCertificates()) {
      when(snapshot.getAccount(certificate.getSignerId())).thenReturn(account);
    }
    for (Signature certificate : validatedTransaction2.getCertificates()) {
      when(snapshot.getAccount(certificate.getSignerId())).thenReturn(account);
    }
    ingestEngine = new IngestEngine(state, blocks, transactionIds, pendingTransactions, seenEntities, assignment);

    AtomicInteger threadError = new AtomicInteger();
    ArrayList<ValidatedTransaction> concTrxList = new ArrayList<>();
    concTrxList.add(validatedTransaction1);
    concTrxList.add(validatedTransaction2);
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
    verify(pendingTransactions, times(1)).add(validatedTransaction1);
    verify(pendingTransactions, times(1)).add(validatedTransaction2);
    Assertions.assertEquals(0, threadError.get());
  }

  // 13. Happy path of receiving a duplicate pair of validated transactions sequentially.
  @Test // 13.
  public void testValidatedSameTwoTransactions() {
    // ingest engine set up
    blocks = mock(Blocks.class);
    state = mock(State.class);
    pendingTransactions = mock(Transactions.class);
    snapshot = mock(Snapshot.class);
    assignment = mock(Assignment.class);
    transactionIds = mock(Identifiers.class);
    seenEntities = mock(Identifiers.class);
    ValidatedTransaction validatedTransaction = ValidatedTransactionFixture.newValidatedTransaction();

    when(state.atBlockId(validatedTransaction.getReferenceBlockId())).thenReturn(snapshot);
    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all certificates
    PublicKey pubKey = mock(PublicKey.class); // mock public key
    Account account = mock(Account.class); // mock account
    when(account.getPublicKey()).thenReturn(pubKey); // returns the mocked public key for all accounts
    // returns true for all signatures
    when(pubKey.verifySignature(any(Transaction.class), any(Signature.class))).thenReturn(true);
    for (Signature certificate : validatedTransaction.getCertificates()) {
      when(snapshot.getAccount(certificate.getSignerId())).thenReturn(account);
    }
    ingestEngine = new IngestEngine(state, blocks, transactionIds, pendingTransactions, seenEntities, assignment);
    ingestEngine.process(validatedTransaction);
    when(seenEntities.has(validatedTransaction.id())).thenReturn(true);
    ingestEngine.process(validatedTransaction);
    verify(pendingTransactions, times(1)).add(validatedTransaction);
  }

  // 14. Happy path of receiving two duplicate pair of validated transactions concurrently.
  @Test
  public void testValidatedConcurrentDuplicateTwoTransactions() {
    // ingest engine set up
    blocks = mock(Blocks.class);
    state = mock(State.class);
    pendingTransactions = mock(Transactions.class);
    snapshot = mock(Snapshot.class);
    assignment = mock(Assignment.class);
    transactionIds = mock(Identifiers.class);
    seenEntities = mock(Identifiers.class);
    ValidatedTransaction validatedTransaction = ValidatedTransactionFixture.newValidatedTransaction();
    when(state.atBlockId(validatedTransaction.getReferenceBlockId())).thenReturn(snapshot);
    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all certificates
    PublicKey pubKey = mock(PublicKey.class); // mock public key
    Account account = mock(Account.class); // mock account
    when(account.getPublicKey()).thenReturn(pubKey); // returns the mocked public key for all accounts
    // returns true for all signatures
    when(pubKey.verifySignature(any(Transaction.class), any(Signature.class))).thenReturn(true);
    for (Signature certificate : validatedTransaction.getCertificates()) {
      when(snapshot.getAccount(certificate.getSignerId())).thenReturn(account);
    }
    ingestEngine = new IngestEngine(state, blocks, transactionIds, pendingTransactions, seenEntities, assignment);

    AtomicInteger threadError = new AtomicInteger();
    int concurrencyDegree = 2;
    CountDownLatch threadsDone = new CountDownLatch(concurrencyDegree);
    Thread[] threads = new Thread[concurrencyDegree];
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      threads[i] = new Thread(() -> {
        try {
          ingestEngine.process(validatedTransaction);
          when(seenEntities.has(validatedTransaction.id())).thenReturn(true);
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
    verify(pendingTransactions, times(1)).add(validatedTransaction);
    Assertions.assertEquals(0, threadError.get());
  }

  // 15. Happy path of receiving a validated transaction that its id already exists in txHash.
  @Test // 15.
  public void testValidatedTransactionAlreadyInTxHash() {
    // ingest engine set up
    blocks = mock(Blocks.class);
    state = mock(State.class);
    pendingTransactions = mock(Transactions.class);
    snapshot = mock(Snapshot.class);
    assignment = mock(Assignment.class);
    transactionIds = mock(Identifiers.class);
    seenEntities = mock(Identifiers.class);
    ValidatedTransaction validatedTransaction = ValidatedTransactionFixture.newValidatedTransaction();

    when(state.atBlockId(validatedTransaction.getReferenceBlockId())).thenReturn(snapshot);
    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all certificates
    PublicKey pubKey = mock(PublicKey.class); // mock public key
    Account account = mock(Account.class); // mock account
    when(account.getPublicKey()).thenReturn(pubKey); // returns the mocked public key for all accounts
    // returns true for all signatures
    when(pubKey.verifySignature(any(Transaction.class), any(Signature.class))).thenReturn(true);
    for (Signature certificate : validatedTransaction.getCertificates()) {
      when(snapshot.getAccount(certificate.getSignerId())).thenReturn(account);
    }
    // simulates that the transaction is already in txHash
    when(transactionIds.has(validatedTransaction.id())).thenReturn(true);
    ingestEngine = new IngestEngine(state, blocks, transactionIds, pendingTransactions, seenEntities, assignment);
    ingestEngine.process(validatedTransaction);
    verify(pendingTransactions, times(0)).add(validatedTransaction);
  }

  // 16. Happy path of receiving a validated transaction that its id already exists in pendingTx.
  @Test // 16.
  public void testValidatedTransactionAlreadyInPendingTx() {
    // ingest engine set up
    blocks = mock(Blocks.class);
    state = mock(State.class);
    pendingTransactions = mock(Transactions.class);
    snapshot = mock(Snapshot.class);
    assignment = mock(Assignment.class);
    transactionIds = mock(Identifiers.class);
    seenEntities = mock(Identifiers.class);
    ValidatedTransaction validatedTransaction = ValidatedTransactionFixture.newValidatedTransaction();

    when(state.atBlockId(validatedTransaction.getReferenceBlockId())).thenReturn(snapshot);
    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all certificates
    PublicKey pubKey = mock(PublicKey.class); // mock public key
    Account account = mock(Account.class); // mock account
    when(account.getPublicKey()).thenReturn(pubKey); // returns the mocked public key for all accounts
    // returns true for all signatures
    when(pubKey.verifySignature(any(Transaction.class), any(Signature.class))).thenReturn(true);
    for (Signature certificate : validatedTransaction.getCertificates()) {
      when(snapshot.getAccount(certificate.getSignerId())).thenReturn(account);
    }
    // simulates that the transaction is already in txHash
    when(pendingTransactions.has(validatedTransaction.id())).thenReturn(true);
    ingestEngine = new IngestEngine(state, blocks, transactionIds, pendingTransactions, seenEntities, assignment);
    ingestEngine.process(validatedTransaction);
    verify(pendingTransactions, times(0)).add(validatedTransaction);
  }

  // 17. Unhappy path of receiving an entity that is neither a validated block nor a validated transaction.
  @Test // 17.
  public void testNeitherBlockNorTransaction() {
    Entity e = new EntityFixture(); // not a block nor a transaction
    ingestEngine = new IngestEngine(state, blocks, transactionIds, pendingTransactions, seenEntities, assignment);
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      ingestEngine.process(e);
    });
  }

  // 18. Happy path of receiving a validated transaction and a validated block concurrently
  //     (block does not contain that transaction).
  @Test // 18.
  public void testConcurrentValidatedTransactionAndBlockNonOverlapping() {
    // ingest engine set up
    blocks = mock(Blocks.class);
    state = mock(State.class);
    pendingTransactions = mock(Transactions.class);
    snapshot = mock(Snapshot.class);
    assignment = mock(Assignment.class);
    transactionIds = mock(Identifiers.class);
    seenEntities = mock(Identifiers.class);
    ValidatedTransaction validatedTransaction = ValidatedTransactionFixture.newValidatedTransaction();
    ValidatedBlock block = ValidatedBlockFixture.newValidatedBlock();
    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());

    when(snapshot.all()).thenReturn(accounts);
    when(state.atBlockId(block.getPreviousBlockId())).thenReturn(snapshot);
    when(state.atBlockId(validatedTransaction.getReferenceBlockId())).thenReturn(snapshot);
    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all certificates
    PublicKey pubKey = mock(PublicKey.class); // mock public key
    Account account = mock(Account.class); // mock account
    when(snapshot.getAccount(any(Identifier.class))).thenReturn(account);
    when(account.getPublicKey()).thenReturn(pubKey); // returns the mocked public key for all accounts
    // returns true for all signatures
    when(pubKey.verifySignature(any(Entity.class), any(Signature.class))).thenReturn(true);
    ingestEngine = new IngestEngine(state, blocks, transactionIds, pendingTransactions, seenEntities, assignment);

    AtomicInteger threadError = new AtomicInteger();
    ArrayList<Entity> concList = new ArrayList<>();
    concList.add(block);
    concList.add(validatedTransaction);
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
    Assertions.assertEquals(0, threadError.get());
    verify(pendingTransactions, times(1)).add(validatedTransaction);
    verify(blocks, times(1)).add(block);
  }

  // 19. Happy path of receiving a validated transaction and a validated block concurrently
  //     (block does contain the transaction).
  @Test // 19.
  public void testConcurrentValidatedTransactionAndBlockOverlapping() {
    // ingest engine set up
    blocks = mock(Blocks.class);
    state = mock(State.class);
    pendingTransactions = mock(Transactions.class);
    snapshot = mock(Snapshot.class);
    assignment = mock(Assignment.class);
    transactionIds = mock(Identifiers.class);
    seenEntities = mock(Identifiers.class);
    ValidatedBlock block = ValidatedBlockFixture.newValidatedBlock();
    ValidatedTransaction validatedTransaction = block.getTransactions()[0]; // block contains the transaction
    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());

    when(snapshot.all()).thenReturn(accounts);
    when(state.atBlockId(block.getPreviousBlockId())).thenReturn(snapshot);
    when(state.atBlockId(validatedTransaction.getReferenceBlockId())).thenReturn(snapshot);
    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all certificates
    PublicKey pubKey = mock(PublicKey.class); // mock public key
    Account account = mock(Account.class); // mock account
    when(snapshot.getAccount(any(Identifier.class))).thenReturn(account);
    when(account.getPublicKey()).thenReturn(pubKey); // returns the mocked public key for all accounts
    // returns true for all signatures
    when(pubKey.verifySignature(any(Entity.class), any(Signature.class))).thenReturn(true);
    ingestEngine = new IngestEngine(state, blocks, transactionIds, pendingTransactions, seenEntities, assignment);

    AtomicInteger threadError = new AtomicInteger();
    ArrayList<Entity> concList = new ArrayList<>();
    concList.add(block);
    concList.add(validatedTransaction);
    int concurrencyDegree = 2;
    CountDownLatch threadsDone = new CountDownLatch(concurrencyDegree);
    Thread[] threads = new Thread[concurrencyDegree];
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      threads[i] = new Thread(() -> {
        try {
          ingestEngine.process(concList.get(finalI));
          when(transactionIds.has(validatedTransaction.id())).thenReturn(true);
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
    verify(pendingTransactions, times(0)).add(validatedTransaction);
    verify(blocks, times(1)).add(block);
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
