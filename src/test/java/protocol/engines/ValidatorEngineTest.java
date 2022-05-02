package protocol.engines;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import model.crypto.KeyGen;
import model.crypto.PrivateKey;
import model.crypto.PublicKey;
import model.crypto.Signature;
import model.exceptions.LightChainDistributedStorageException;
import model.lightchain.*;
import model.local.Local;
import network.Network;
import network.NetworkAdapter;
import networking.MockConduit;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import state.Snapshot;
import state.State;
import unittest.fixtures.*;
import unittest.fixtures.ValidatedTransactionFixture;

/**
 * Encapsulates tests for validator engine.
 */
public class ValidatorEngineTest {
  // a single individual test function for each of these scenarios:
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
  //++ 15. Happy path of receiving a transaction that already been validated (second transaction should be discarded).
  //++ 16. Unhappy path of receiving an entity that is neither a block nor a transaction.
  // 17. Happy path of receiving a transaction and a block concurrently (block does not contain that transaction).
  // 18. Happy path of receiving a transaction and a block concurrently (block does contain the transaction).
  // 19. Unhappy path of receiving an invalid transaction (one test per each validation criteria, e.g., correctness,
  //     soundness, etc). Invalid transaction should be discarded without sending back a signature to its sender.
  // 19. Unhappy path of receiving an invalid block (one test per each validation criteria, e.g., correctness,
  //     soundness, etc). Invalid block should be discarded without sending back a signature to its proposer.
  Random random = new Random();
  ValidatorEngine engine;
  Identifier localId;
  PrivateKey privateKey;
  PublicKey publicKey;
  Local local;
  Network net;
  NetworkAdapter networkAdapter;
  MockConduit conduit;
  Block prevBlock;
  Block block;
  State state;
  Snapshot snapshot;
  Snapshot prevSnapshot;
  Snapshot senderLastSnapshot1;
  Snapshot senderLastSnapshot2;
  ArrayList<Account> accounts;
  public void setup(){
    localId = IdentifierFixture.newIdentifier();
    KeyGen kg = KeyGenFixture.newKeyGen();
    privateKey = kg.getPrivateKey();
    publicKey = kg.getPublicKey();
    local = new Local(localId, privateKey);

    net = mock(Network.class);
    networkAdapter = mock(NetworkAdapter.class);
    conduit = new MockConduit("validator", networkAdapter);

    prevBlock = BlockFixture.newBlock();
    block = BlockFixture.newBlock(prevBlock.id(), prevBlock.getHeight() + 1);

    state = mock(State.class);
    snapshot = mock(Snapshot.class);
    prevSnapshot = mock(Snapshot.class);
    senderLastSnapshot1 = mock(Snapshot.class);
    senderLastSnapshot2 = mock(Snapshot.class);
    when(state.atBlockId(block.id())).thenReturn(snapshot);
    when(state.atBlockId(prevBlock.id())).thenReturn(prevSnapshot);
    when(snapshot.getReferenceBlockHeight()).thenReturn((long) block.getHeight());
    when(snapshot.getReferenceBlockId()).thenReturn(block.id());
    when(prevSnapshot.getReferenceBlockId()).thenReturn(prevBlock.id());
    when(prevSnapshot.getReferenceBlockHeight()).thenReturn((long) prevBlock.getHeight());
  }


  @Test
  public void testReceiveOneValidBlock() {
    // Arrange
    setup();
    // Creates accounts for the snapshot including an account with the local id.
    accounts = new ArrayList<>(AccountFixture.newAccounts(localId, 10, 10).values());
    ArrayList<Identifier> accountIds = new ArrayList<>(accounts.size());
    for (int i = 0; i < accounts.size(); i++) {
      accountIds.add(accounts.get(i).getIdentifier());
    }

    ValidatedBlock validatedBlock = ValidatedBlockFixture.newValidatedBlock(accounts, prevBlock.getHeight() + 1, prevBlock.id());


    when(snapshot.all()).thenReturn(accounts);
    when(state.atBlockId(validatedBlock.getPreviousBlockId())).thenReturn(snapshot);
    for (ValidatedTransaction tx : validatedBlock.getTransactions()) {
      Account senderAccount = accounts.get(accountIds.indexOf(tx.getSender()));
      when(state.atBlockId(senderAccount.getLastBlockId())).thenReturn(prevSnapshot);
      when(state.atBlockId(tx.getReferenceBlockId())).thenReturn(snapshot);
    }
    when(net.register(any(ValidatorEngine.class), eq("validator"))).thenReturn(conduit);
    engine = new ValidatorEngine(net, local, state);



    try {
      engine.process(validatedBlock);
    } catch (IllegalArgumentException ex) {
      Assertions.fail(ex);
    }
    Assertions.assertNotNull(validatedBlock.getSignature());
    //System.out.println(local.signEntity(transaction).id());
    //TODO: assert has sent what?
    //Assertions.assertTrue(conduit.hasSent(transaction.getSignature().id()));


  }

  @Test
  public void testReceiveOneValidTransaction() {
    // Arrange
    setup();

    // Creates accounts for the snapshot including an account with the local id.
    accounts = new ArrayList<>(AccountFixture.newAccounts(localId, 10, 10).values());
    when(snapshot.all()).thenReturn(accounts);
    for (int i = 0; i < accounts.size(); i++) {
      when(snapshot.getAccount(snapshot.all().get(i).getIdentifier())).thenReturn(accounts.get(i));
    }


    when(net.register(any(ValidatorEngine.class), eq("validator"))).thenReturn(conduit);
    engine = new ValidatorEngine(net, local, state);

    ArrayList<ValidatedTransaction> transactions = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      Identifier signer = snapshot.all().get(random.nextInt(accounts.size())).getIdentifier();
      System.out.println("sgner "+signer);
      ValidatedTransaction validatedTransaction = ValidatedTransactionFixture.newValidatedTransaction(
          block.id(),
          snapshot.all().get(i).getIdentifier(),
          snapshot.all().get(i + 1).getIdentifier(),
          snapshot.all().get(random.nextInt(accounts.size())).getIdentifier());

      when(snapshot.getAccount(signer).getPublicKey()
          .verifySignature(validatedTransaction, validatedTransaction.getSignature())).thenReturn(true);
      System.out.println(snapshot.getAccount(signer).getPublicKey());
      snapshot.all().get(i).setBalance(validatedTransaction.getAmount() * 10 + 1);
      transactions.add(validatedTransaction);

    }
    System.out.println("sgner2 "+transactions.get(0).getSignature().getSignerId());
    System.out.println("de "+ snapshot.getAccount(transactions.get(0).getSignature().getSignerId()).getPublicKey());
    when(state.atBlockId(snapshot.all().get(0).getLastBlockId())).thenReturn(senderLastSnapshot1);
    when(state.atBlockId(snapshot.all().get(1).getLastBlockId())).thenReturn(senderLastSnapshot2);


    try {
      engine.process(transactions.get(0));
    } catch (IllegalArgumentException ex) {
      Assertions.fail(ex);
    }
    Assertions.assertNotNull(transactions.get(0).getSignature());
    //System.out.println(local.signEntity(transaction).id());
    //TODO: assert has sent what?
    //Assertions.assertTrue(conduit.hasSent(transaction.getSignature().id()));
   // System.out.println("ss "+ transactions.get(0).getSignature().getSignerId());
    System.out.println(snapshot.getAccount(transactions.get(0).getSignature().getSignerId()).getPublicKey().verifySignature(transactions.get(0), transactions.get(0).getSignature()));


  }

  @Test
  public void testReceiveTwoValidTransactionConcurrently() {
    // Arrange
    setup();

    // Creates accounts for the snapshot including an account with the local id.
    accounts = new ArrayList<>(AccountFixture.newAccounts(localId, 10, 10).values());
    when(snapshot.all()).thenReturn(accounts);
    when(net.register(any(ValidatorEngine.class), eq("validator"))).thenReturn(conduit);
    engine = new ValidatorEngine(net, local, state);

    ArrayList<ValidatedTransaction> transactions = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      ValidatedTransaction validatedTransaction = ValidatedTransactionFixture.newValidatedTransaction(
          block.id(), snapshot.all().get(i).getIdentifier(), snapshot.all().get(i + 1).getIdentifier());
      snapshot.all().get(i).setBalance(validatedTransaction.getAmount() * 10 + 1);
      transactions.add(validatedTransaction);
      when(snapshot.getAccount(snapshot.all().get(i).getIdentifier())).thenReturn(accounts.get(i));
      when(snapshot.getAccount(snapshot.all().get(i + 1).getIdentifier())).thenReturn(accounts.get(i + 1));
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
      //System.out.println(local.signEntity(transaction).id());
      //TODO: assert has sent what?
      //Assertions.assertTrue(conduit.hasSent(transaction.getSignature().id()));
    }
  }

  @Test
  public void testReceiveTwoValidTransactionSequentially() {
    // Arrange
    setup();

    // Creates accounts for the snapshot including an account with the local id.
    accounts = new ArrayList<>(AccountFixture.newAccounts(localId, 10, 10).values());
    when(snapshot.all()).thenReturn(accounts);
    when(net.register(any(ValidatorEngine.class), eq("validator"))).thenReturn(conduit);
    engine = new ValidatorEngine(net, local, state);

    ArrayList<ValidatedTransaction> transactions = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      ValidatedTransaction validatedTransaction = ValidatedTransactionFixture.newValidatedTransaction(
          block.id(), snapshot.all().get(i).getIdentifier(), snapshot.all().get(i + 1).getIdentifier());
      snapshot.all().get(i).setBalance(validatedTransaction.getAmount() * 10 + 1);
      transactions.add(validatedTransaction);
      when(snapshot.getAccount(snapshot.all().get(i).getIdentifier())).thenReturn(accounts.get(i));
      when(snapshot.getAccount(snapshot.all().get(i + 1).getIdentifier())).thenReturn(accounts.get(i + 1));
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
    setup();
    // Creates accounts for the snapshot including an account with the local id.
    accounts = new ArrayList<>(AccountFixture.newAccounts(localId, 10, 10).values());
    when(snapshot.all()).thenReturn(accounts);
    when(net.register(any(ValidatorEngine.class), eq("validator"))).thenReturn(conduit);
    engine = new ValidatorEngine(net, local, state);

    ValidatedTransaction validatedTransaction = ValidatedTransactionFixture.newValidatedTransaction(
        block.id(), snapshot.all().get(0).getIdentifier(), snapshot.all().get(1).getIdentifier());
    snapshot.all().get(0).setBalance(validatedTransaction.getAmount() * 10 + 1);

    when(snapshot.getAccount(snapshot.all().get(0).getIdentifier())).thenReturn(accounts.get(0));
    when(snapshot.getAccount(snapshot.all().get(0).getIdentifier()).getPublicKey()
        .verifySignature(validatedTransaction, validatedTransaction.getSignature())).thenReturn(true);

    when(state.atBlockId(snapshot.all().get(0).getLastBlockId())).thenReturn(senderLastSnapshot1);

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
      Assertions.assertEquals(conduit.allEntities().size(), 1);
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }

  }

  @Test
  public void testReceiveTwoDuplicateTransactionConcurrently() {
    // Arrange
    setup();
    // Creates accounts for the snapshot including an account with the local id.
    accounts = new ArrayList<>(AccountFixture.newAccounts(localId, 10, 10).values());
    when(snapshot.all()).thenReturn(accounts);
    when(net.register(any(ValidatorEngine.class), eq("validator"))).thenReturn(conduit);
    engine = new ValidatorEngine(net, local, state);

    ValidatedTransaction validatedTransaction = ValidatedTransactionFixture.newValidatedTransaction(
        block.id(), snapshot.all().get(0).getIdentifier(), snapshot.all().get(1).getIdentifier());
    snapshot.all().get(0).setBalance(validatedTransaction.getAmount() * 10 + 1);

    when(snapshot.getAccount(snapshot.all().get(0).getIdentifier())).thenReturn(accounts.get(0));
    when(snapshot.getAccount(snapshot.all().get(0).getIdentifier()).getPublicKey()
        .verifySignature(validatedTransaction, validatedTransaction.getSignature())).thenReturn(true);

    when(state.atBlockId(snapshot.all().get(0).getLastBlockId())).thenReturn(senderLastSnapshot1);

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
      Assertions.assertEquals(conduit.allEntities().size(), 1);
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }
  }


  @Test
  public void testReceiveNoTransactionNorBlock() {
    // Arrange
    setup();
    // Creates accounts for the snapshot including an account with the local id.
    accounts = new ArrayList<>(AccountFixture.newAccounts(localId, 10, 10).values());
    when(snapshot.all()).thenReturn(accounts);
    when(net.register(any(ValidatorEngine.class), eq("validator"))).thenReturn(conduit);
    engine = new ValidatorEngine(net, local, state);

    // An entity which is neither block nor transaction.
    Signature signature = SignatureFixture.newSignatureFixture();
    try {
      engine.process(signature);
      Assertions.fail("Should have thrown an exception");
    } catch (IllegalArgumentException ex) {
      Assertions.assertEquals(ex.getMessage(), "entity is neither a block nor a transaction");
    }
    Assertions.assertFalse(conduit.hasSent(signature.id()));
  }

}
