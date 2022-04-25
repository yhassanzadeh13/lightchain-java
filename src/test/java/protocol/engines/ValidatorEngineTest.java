package protocol.engines;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import model.crypto.PrivateKey;
import model.exceptions.LightChainDistributedStorageException;
import model.lightchain.*;
import model.local.Local;
import network.Conduit;
import network.Network;
import network.NetworkAdapter;
import networking.MockConduit;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import protocol.Parameters;
import protocol.assigner.LightChainValidatorAssigner;
import state.Snapshot;
import state.State;
import unittest.fixtures.*;

/**
 * Encapsulates tests for validator engine.
 */
public class ValidatorEngineTest {
  // TODO: a single individual test function for each of these scenarios:
  // Note: except when explicitly mentioned, always assume the input block or transaction is assigned to this node
  // i.e., mock the assigner for that.
  // 1. Happy path of receiving a valid single block.
  // 2. Happy path of receiving two valid blocks sequentially.
  // 3. Happy path of receiving two valid blocks concurrently.
  // 4. Happy path of receiving two duplicate blocks sequentially (the second duplicate block should be discarded).
  // 5. Happy path of receiving two duplicate blocks concurrently (the second duplicate block should be discarded).
  // 6. Happy path of receiving a valid block with shared transactions in pendingTx.
  // 7. Happy path of receiving an already validated block,
  //    second block should be discarded right away.
  // 8.  Unhappy path of receiving a transaction and block (sequentially) that is not assigned to this node.
  // 9.  Unhappy path of receiving a transaction and block (concurrently) that is not assigned to this node.
  //++ 10. Happy path of receiving a valid transaction.
  //++ 11. Happy path of receiving two valid transactions sequentially.
  //++ 12. Happy path of receiving two valid transactions concurrently.
  //++ 13. Happy path of receiving a duplicate pair of valid transactions sequentially.
  //++ 14. Happy path of receiving a duplicate pair of valid transactions concurrently.
  // 15. Happy path of receiving a transaction that already been validated (second transaction should be discarded).
  // 16. Unhappy path of receiving an entity that is neither a block nor a transaction.
  // 17. Happy path of receiving a transaction and a block concurrently (block does not contain that transaction).
  // 18. Happy path of receiving a transaction and a block concurrently (block does contain the transaction).
  // 19. Unhappy path of receiving a invalid transaction (one test per each validation criteria, e.g., correctness,
  //     soundness, etc). Invalid transaction should be discarded without sending back a signature to its sender.
  // 19. Unhappy path of receiving a invalid block (one test per each validation criteria, e.g., correctness,
  //     soundness, etc). Invalid block should be discarded without sending back a signature to its proposer.

  @Test
  public void testReceiveTwoValidBlockConcurrently() {
    // Arrange
    Identifier localId = IdentifierFixture.newIdentifier();
    PrivateKey privateKey = KeyGenFixture.newKeyGen().getPrivateKey();
    Local local = new Local(localId, privateKey);

    Network net = mock(Network.class);
    NetworkAdapter netAdapter = mock(NetworkAdapter.class);
    Conduit conduit = new MockConduit("validator", netAdapter);
    State state = mock(State.class);

    ValidatorEngine engine = new ValidatorEngine(net, local, state);
    when(net.register(engine, "validator")).thenReturn(conduit);

    ArrayList<ValidatedBlock> blocks = null;
    /*for (int i = 0; i < 2; i++) {
      blocks.add(BlockFixture.newBlock());
    }

    AtomicInteger threadErrorCount = new AtomicInteger();
    CountDownLatch done = new CountDownLatch(1);

    Thread[] validationThreads = new Thread[2];
    for(int i =0; i < 2; i++) {
      int finalI = i;
      validationThreads[i] = new Thread(() -> {
        // implement body of thread.
        // if some error happens that leads to test failure:
        // threadErrorCount.getAndIncrement();
        try {
          engine.process(blocks[finalI]);
        } catch (IllegalArgumentException ex){
          threadErrorCount.incrementAndGet();
        }


        done.countDown();
      });
    }

    // run threads
    for (Thread t : validationThreads) {
      t.start();
    }

    try {
      boolean doneOnTime = done.await(10, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOnTime);
    } catch (InterruptedException e) {
      Assertions.fail(e);
    }

    Assertions.assertEquals(0, threadErrorCount.get());*/
  }

  ValidatorEngine engine;

  @Test
  public void testReceiveTwoValidTransactionConcurrently() {
    // Arrange
    Identifier localId = IdentifierFixture.newIdentifier();
    PrivateKey privateKey = KeyGenFixture.newKeyGen().getPrivateKey();
    Local local = new Local(localId, privateKey);

    Network net = mock(Network.class);
    NetworkAdapter networkAdapter = mock(NetworkAdapter.class);
    MockConduit conduit = new MockConduit("validator", networkAdapter);

    Block prevBlock = BlockFixture.newBlock();
    Block block = BlockFixture.newBlock(prevBlock.id(), prevBlock.getHeight() + 1);

    State state = mock(State.class);
    Snapshot snapshot = mock(Snapshot.class);
    Snapshot prevSnapshot = mock(Snapshot.class);
    Snapshot senderLastSnapshot1 = mock(Snapshot.class);
    Snapshot senderLastSnapshot2 = mock(Snapshot.class);

    when(state.atBlockId(block.id())).thenReturn(snapshot);
    when(state.atBlockId(prevBlock.id())).thenReturn(prevSnapshot);

    // Creates accounts for the snapshot including an account with the local Id.
    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(localId, 10, 10).values());
    when(snapshot.all()).thenReturn(accounts);
    when(snapshot.getReferenceBlockHeight()).thenReturn((long) block.getHeight());
    when(snapshot.getReferenceBlockId()).thenReturn(block.id());

    when(prevSnapshot.getReferenceBlockId()).thenReturn(prevBlock.id());
    when(prevSnapshot.getReferenceBlockHeight()).thenReturn((long) prevBlock.getHeight());

    when(net.register(engine, "validator")).thenReturn(conduit);
    engine = new ValidatorEngine(net, local, state);

    ArrayList<ValidatedTransaction> transactions = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      ValidatedTransaction validatedTransaction = ValidatedTransactionFixture.newValidatedTransaction(
          block.id(), snapshot.all().get(i).getIdentifier(), snapshot.all().get(i + 1).getIdentifier());
      snapshot.all().get(i).setBalance(validatedTransaction.getAmount() * 10 + 1);
      transactions.add(validatedTransaction);
      when(snapshot.getAccount(snapshot.all().get(i).getIdentifier())).thenReturn(accounts.get(i));
      when(snapshot.getAccount(snapshot.all().get(i + 1).getIdentifier())).thenReturn(accounts.get(i + 1));
      // TODO: should this auth be handled while creating the fixtures?
      when(snapshot.getAccount(snapshot.all().get(i).getIdentifier()).getPublicKey()
          .verifySignature(validatedTransaction, validatedTransaction.getSignature())).thenReturn(true);
    }
    when(state.atBlockId(snapshot.all().get(0).getLastBlockId())).thenReturn(senderLastSnapshot1);
    when(state.atBlockId(snapshot.all().get(1).getLastBlockId())).thenReturn(senderLastSnapshot2);


    AtomicInteger threadErrorCount = new AtomicInteger();
    CountDownLatch done = new CountDownLatch(1);
    Thread[] validationThreads = new Thread[2];
    for (int i = 0; i < 2; i++) {
      int finalI = i;
      validationThreads[i] = new Thread(() -> {
        try {
          engine.process(transactions.get(finalI));
        } catch (IllegalArgumentException ex) {
          threadErrorCount.incrementAndGet();
        }
        done.countDown();
      });
    }

    // run threads
    for (Thread t : validationThreads) {
      t.start();
    }

    try {
      boolean doneOnTime = done.await(10, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOnTime);
    } catch (InterruptedException e) {
      Assertions.fail(e);
    }

    Assertions.assertEquals(0, threadErrorCount.get());
    for (ValidatedTransaction transaction : transactions) {
      Assertions.assertNotNull(transaction.getSignature());
      Assertions.assertTrue(conduit.hasSent(transaction.id()));
    }

  }

  @Test
  public void testReceiveTwoValidTransactionSequentially() {
    // Arrange
    Identifier localId = IdentifierFixture.newIdentifier();
    PrivateKey privateKey = KeyGenFixture.newKeyGen().getPrivateKey();
    Local local = new Local(localId, privateKey);

    Network net = mock(Network.class);
    NetworkAdapter networkAdapter = mock(NetworkAdapter.class);
    MockConduit conduit = new MockConduit("validator", networkAdapter);

    Block prevBlock = BlockFixture.newBlock();
    Block block = BlockFixture.newBlock(prevBlock.id(), prevBlock.getHeight() + 1);

    State state = mock(State.class);
    Snapshot snapshot = mock(Snapshot.class);
    Snapshot prevSnapshot = mock(Snapshot.class);
    Snapshot senderLastSnapshot1 = mock(Snapshot.class);
    Snapshot senderLastSnapshot2 = mock(Snapshot.class);

    when(state.atBlockId(block.id())).thenReturn(snapshot);
    when(state.atBlockId(prevBlock.id())).thenReturn(prevSnapshot);

    // Creates accounts for the snapshot including an account with the local Id.
    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(localId, 10, 10).values());
    when(snapshot.all()).thenReturn(accounts);
    when(snapshot.getReferenceBlockHeight()).thenReturn((long) block.getHeight());
    when(snapshot.getReferenceBlockId()).thenReturn(block.id());

    when(prevSnapshot.getReferenceBlockId()).thenReturn(prevBlock.id());
    when(prevSnapshot.getReferenceBlockHeight()).thenReturn((long) prevBlock.getHeight());

    when(net.register(engine, "validator")).thenReturn(conduit);
    engine = new ValidatorEngine(net, local, state);

    ArrayList<ValidatedTransaction> transactions = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      ValidatedTransaction validatedTransaction = ValidatedTransactionFixture.newValidatedTransaction(
          block.id(), snapshot.all().get(i).getIdentifier(), snapshot.all().get(i + 1).getIdentifier());
      snapshot.all().get(i).setBalance(validatedTransaction.getAmount() * 10 + 1);
      transactions.add(validatedTransaction);
      when(snapshot.getAccount(snapshot.all().get(i).getIdentifier())).thenReturn(accounts.get(i));
      when(snapshot.getAccount(snapshot.all().get(i + 1).getIdentifier())).thenReturn(accounts.get(i + 1));
      // TODO: should this auth be handled while creating the fixtures?
      when(snapshot.getAccount(snapshot.all().get(i).getIdentifier()).getPublicKey()
          .verifySignature(validatedTransaction, validatedTransaction.getSignature())).thenReturn(true);
    }
    when(state.atBlockId(snapshot.all().get(0).getLastBlockId())).thenReturn(senderLastSnapshot1);
    when(state.atBlockId(snapshot.all().get(1).getLastBlockId())).thenReturn(senderLastSnapshot2);

    for (int i = 0; i < 2; i++) {
      try {
        engine.process(transactions.get(i));
      } catch (IllegalArgumentException ex) {
        Assertions.fail(ex);
      }
    }
    for (ValidatedTransaction transaction : transactions) {
      Assertions.assertNotNull(transaction.getSignature());
      Assertions.assertTrue(conduit.hasSent(transaction.id()));
    }
  }

  @Test
  public void testReceiveTwoDuplicateTransactionSequentially() {
    // Arrange
    Identifier localId = IdentifierFixture.newIdentifier();
    PrivateKey privateKey = KeyGenFixture.newKeyGen().getPrivateKey();
    Local local = new Local(localId, privateKey);

    Network net = mock(Network.class);
    NetworkAdapter networkAdapter = mock(NetworkAdapter.class);
    MockConduit conduit = new MockConduit("validator", networkAdapter);

    Block prevBlock = BlockFixture.newBlock();
    Block block = BlockFixture.newBlock(prevBlock.id(), prevBlock.getHeight() + 1);

    State state = mock(State.class);
    Snapshot snapshot = mock(Snapshot.class);
    Snapshot prevSnapshot = mock(Snapshot.class);
    Snapshot senderLastSnapshot = mock(Snapshot.class);

    when(state.atBlockId(block.id())).thenReturn(snapshot);
    when(state.atBlockId(prevBlock.id())).thenReturn(prevSnapshot);

    // Creates accounts for the snapshot including an account with the local Id.
    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(localId, 10, 10).values());
    when(snapshot.all()).thenReturn(accounts);
    when(snapshot.getReferenceBlockHeight()).thenReturn((long) block.getHeight());
    when(snapshot.getReferenceBlockId()).thenReturn(block.id());

    when(prevSnapshot.getReferenceBlockId()).thenReturn(prevBlock.id());
    when(prevSnapshot.getReferenceBlockHeight()).thenReturn((long) prevBlock.getHeight());

    when(net.register(engine, "validator")).thenReturn(conduit);
    engine = new ValidatorEngine(net, local, state);

    ValidatedTransaction validatedTransaction = ValidatedTransactionFixture.newValidatedTransaction(
        block.id(), snapshot.all().get(0).getIdentifier(), snapshot.all().get(1).getIdentifier());
    snapshot.all().get(0).setBalance(validatedTransaction.getAmount() * 10 + 1);

    when(snapshot.getAccount(snapshot.all().get(0).getIdentifier())).thenReturn(accounts.get(0));
    // TODO: should this auth be handled while creating the fixtures?
    when(snapshot.getAccount(snapshot.all().get(0).getIdentifier()).getPublicKey()
        .verifySignature(validatedTransaction, validatedTransaction.getSignature())).thenReturn(true);

    when(state.atBlockId(snapshot.all().get(0).getLastBlockId())).thenReturn(senderLastSnapshot);

    // First
      try {
        engine.process(validatedTransaction);
      } catch (IllegalArgumentException ex) {
        Assertions.fail(ex);
      }
    Assertions.assertNotNull(validatedTransaction.getSignature());
    Assertions.assertTrue(conduit.hasSent(validatedTransaction.id()));
    // Second, where we check that the process doesn't follow through
    try {
      engine.process(validatedTransaction);
    } catch (IllegalArgumentException ex) {
      Assertions.fail(ex);
    }

    try {
      Assertions.assertTrue(conduit.allEntities().size() == 1);
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }

  }

  @Test
  public void testReceiveTwoDuplicateTransactionConcurrently() {
    // Arrange
    Identifier localId = IdentifierFixture.newIdentifier();
    PrivateKey privateKey = KeyGenFixture.newKeyGen().getPrivateKey();
    Local local = new Local(localId, privateKey);

    Network net = mock(Network.class);
    NetworkAdapter networkAdapter = mock(NetworkAdapter.class);
    MockConduit conduit = new MockConduit("validator", networkAdapter);

    Block prevBlock = BlockFixture.newBlock();
    Block block = BlockFixture.newBlock(prevBlock.id(), prevBlock.getHeight() + 1);

    State state = mock(State.class);
    Snapshot snapshot = mock(Snapshot.class);
    Snapshot prevSnapshot = mock(Snapshot.class);
    Snapshot senderLastSnapshot = mock(Snapshot.class);

    when(state.atBlockId(block.id())).thenReturn(snapshot);
    when(state.atBlockId(prevBlock.id())).thenReturn(prevSnapshot);

    // Creates accounts for the snapshot including an account with the local Id.
    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(localId, 10, 10).values());
    when(snapshot.all()).thenReturn(accounts);
    when(snapshot.getReferenceBlockHeight()).thenReturn((long) block.getHeight());
    when(snapshot.getReferenceBlockId()).thenReturn(block.id());

    when(prevSnapshot.getReferenceBlockId()).thenReturn(prevBlock.id());
    when(prevSnapshot.getReferenceBlockHeight()).thenReturn((long) prevBlock.getHeight());

    when(net.register(engine, "validator")).thenReturn(conduit);
    engine = new ValidatorEngine(net, local, state);

    ValidatedTransaction validatedTransaction = ValidatedTransactionFixture.newValidatedTransaction(
        block.id(), snapshot.all().get(0).getIdentifier(), snapshot.all().get(1).getIdentifier());
    snapshot.all().get(0).setBalance(validatedTransaction.getAmount() * 10 + 1);

    when(snapshot.getAccount(snapshot.all().get(0).getIdentifier())).thenReturn(accounts.get(0));
    // TODO: should this auth be handled while creating the fixtures?
    when(snapshot.getAccount(snapshot.all().get(0).getIdentifier()).getPublicKey()
        .verifySignature(validatedTransaction, validatedTransaction.getSignature())).thenReturn(true);

    when(state.atBlockId(snapshot.all().get(0).getLastBlockId())).thenReturn(senderLastSnapshot);


    AtomicInteger threadErrorCount = new AtomicInteger();
    CountDownLatch done = new CountDownLatch(1);
    Thread[] validationThreads = new Thread[2];
    for (int i = 0; i < 2; i++) {
      validationThreads[i] = new Thread(() -> {
        try {
          engine.process(validatedTransaction);
        } catch (IllegalArgumentException ex) {
          threadErrorCount.incrementAndGet();
        }
        done.countDown();
      });
    }
    // run threads
    for (Thread t : validationThreads) {
      t.start();
    }

    try {
      boolean doneOnTime = done.await(10, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOnTime);
    } catch (InterruptedException e) {
      Assertions.fail(e);
    }

    Assertions.assertNotNull(validatedTransaction.getSignature());
    Assertions.assertTrue(conduit.hasSent(validatedTransaction.id()));
    try {
      Assertions.assertTrue(conduit.allEntities().size() == 1);
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }

  }
}
