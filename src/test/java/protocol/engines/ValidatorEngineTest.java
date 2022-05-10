package protocol.engines;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import model.crypto.PrivateKey;
import model.crypto.Signature;
import model.exceptions.LightChainDistributedStorageException;
import model.lightchain.*;
import model.local.Local;
import network.Channels;
import network.Network;
import network.NetworkAdapter;
import networking.MockConduit;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import state.Snapshot;
import state.State;
import storage.Identifiers;
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
  private static final Random random = new Random();
  private ValidatorEngine engine;
  private Identifier localId;
  private PrivateKey localPrivateKey;
  private Local local;
  private Network network;
  private MockConduit blockConduit;
  private MockConduit txConduit;
  private Identifiers seenEntities;
  private Block block1;
  private Block block2;
  private Block block3;
  private State state;
  private Snapshot snapshot1;
  private Snapshot snapshot2;
  private Snapshot snapshot3;

  private ArrayList<Account> accounts1;
  private ArrayList<Account> accounts2;

  @BeforeEach
  public void setup(){
    // Arrange
    /// Local node
    localId = IdentifierFixture.newIdentifier();
    localPrivateKey = KeyGenFixture.newKeyGen().getPrivateKey();
    local = new Local(localId, localPrivateKey);

    /// Network
    network = mock(Network.class);
    NetworkAdapter networkAdapter = mock(NetworkAdapter.class);
    blockConduit = new MockConduit(Channels.ProposedBlocks, networkAdapter);
    txConduit = new MockConduit(Channels.ProposedTransactions, networkAdapter);

    block1 = BlockFixture.newBlock();
    block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);
    block3 = BlockFixture.newBlock(block2.id(), block2.getHeight() + 1);

    /// State and Snapshots
    state = mock(State.class);
    snapshot1 = mock(Snapshot.class);
    snapshot2 = mock(Snapshot.class);
    snapshot3 = mock(Snapshot.class);

    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());
    when(snapshot3.getReferenceBlockId()).thenReturn(block3.id());

    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());
    when(snapshot3.getReferenceBlockHeight()).thenReturn((long) block3.getHeight());

    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);
    when(state.atBlockId(block3.id())).thenReturn(snapshot3);

    /// Accounts
    /// Create accounts for the snapshot including an account with the local id.
    ArrayList<Account>[] a = AccountFixture.newAccounts(localId, block1.id(), block2.id(), 10, 10);
    accounts1 = a[0]; accounts2 = a[1];
    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts1);
    when(snapshot3.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot3.getAccount(snapshot3.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }
    /*
    for (Account account: snapshot1.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }*/
    for (Account account: snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }
    for (Account account: snapshot3.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot2);
    }
    seenEntities = mock(Identifiers.class);
  }

  @Test
  public void testReceiveOneValidTransaction() {
    setup();
    // Register the network adapter with the network and create engine.
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state,seenEntities);

    Identifier signerId = snapshot2.all().get(random.nextInt(accounts1.size())).getIdentifier();
    Transaction transaction = TransactionFixture.newTransaction(
        block2.id(),
        snapshot2.all().get(0).getIdentifier(),
        snapshot2.all().get(1).getIdentifier(),
        signerId);

    when(state.atBlockId(transaction.getReferenceBlockId()).getAccount(transaction.getSender()).getPublicKey()
        .verifySignature(transaction, transaction.getSignature())).thenReturn(true);
    snapshot2.all().get(0).setBalance(transaction.getAmount() * 10 + 1);

    try {
      engine.process(transaction);
    } catch (IllegalArgumentException ex) {
      Assertions.fail(ex);
    }
    //TODO: assert has sent what?
    Assertions.assertTrue(txConduit.hasSent(transaction.getSignature().id()));
  }

  @Test
  public void testReceiveTwoValidTransactionConcurrently() {
    // Arrange
    setup();

    // Register the network adapter with the network and create engine.
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    Identifier signerId = snapshot2.all().get(random.nextInt(accounts1.size())).getIdentifier();
    ArrayList<Transaction> transactions = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      Transaction transaction = TransactionFixture.newTransaction(
          block2.id(),
          snapshot2.all().get(i).getIdentifier(),
          snapshot2.all().get(i+1).getIdentifier(),
          signerId);
      transactions.add(transaction);
      when(state.atBlockId(transaction.getReferenceBlockId()).getAccount(transaction.getSender()).getPublicKey()
          .verifySignature(transaction, transaction.getSignature())).thenReturn(true);
      snapshot2.all().get(i).setBalance(transaction.getAmount() * 10 + 1);
    }

    // Act
    /// Create two threads that will process the transactions concurrently.
    int concurrencyDegree = 2;
    AtomicInteger threadErrorCount = new AtomicInteger();
    CountDownLatch done = new CountDownLatch(1);
    Thread[] validationThreads = new Thread[2];
    for (int i = 0; i < concurrencyDegree; i++) {
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

    ///  Run Threads
    for (Thread t : validationThreads) {
      t.start();
    }

    /// Assert done on time and got no errors
    try {
      boolean doneOnTime = done.await(10, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOnTime);
    } catch (InterruptedException e) {
      Assertions.fail(e);
    }
    Assertions.assertEquals(0, threadErrorCount.get());


    for (Transaction transaction : transactions) {
      //Assertions.assertNotNull(transaction.getSignature());
      //TODO: assert has sent what?
    }
  }
/*
  @Test
  public void testReceiveOneValidBlock() {
    // Create accounts for the snapshot including an account with the local id.
    accounts = new ArrayList<>(AccountFixture.newAccounts(localId, 10, 10).values());
    ArrayList<Identifier> accountIds = new ArrayList<>(accounts.size());
    for (Account account : accounts) {
      accountIds.add(account.getIdentifier());
    }

    ValidatedBlock validatedBlock = ValidatedBlockFixture.newValidatedBlock(accounts, prevBlock.getHeight() + 1, prevBlock.id());


    when(snapshot.all()).thenReturn(accounts);
    when(state.atBlockId(validatedBlock.getPreviousBlockId())).thenReturn(snapshot);
    for (ValidatedTransaction tx : validatedBlock.getTransactions()) {
      Account senderAccount = accounts.get(accountIds.indexOf(tx.getSender()));
      when(state.atBlockId(senderAccount.getLastBlockId())).thenReturn(prevSnapshot);
      when(state.atBlockId(tx.getReferenceBlockId())).thenReturn(snapshot);
    }
    when(network.register(any(ValidatorEngine.class), eq("validator"))).thenReturn(blockConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);



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
  public void testReceiveTwoValidTransactionSequentially() {
    // Arrange
    setup();

    // Create accounts for the snapshot including an account with the local id.
    accounts = new ArrayList<>(AccountFixture.newAccounts(localId, 10, 10).values());
    when(snapshot.all()).thenReturn(accounts);
    when(network.register(any(ValidatorEngine.class), eq("validator"))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

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
      Assertions.assertTrue(txConduit.hasSent(transaction.id()));
    }
  }

  @Test
  public void testReceiveTwoDuplicateTransactionSequentially() {
    // Arrange
    setup();
    // Create accounts for the snapshot including an account with the local id.
    accounts = new ArrayList<>(AccountFixture.newAccounts(localId, 10, 10).values());
    when(snapshot.all()).thenReturn(accounts);
    when(network.register(any(ValidatorEngine.class), eq("validator"))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

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
    Assertions.assertTrue(txConduit.hasSent(validatedTransaction.id()));
    // Second, where we check that the process doesn't follow through
    try {
      engine.process(validatedTransaction);
    } catch (IllegalArgumentException ex) {
      Assertions.fail(ex);
    }

    try {
      Assertions.assertEquals(txConduit.allEntities().size(), 1);
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }

  }

  @Test
  public void testReceiveTwoDuplicateTransactionConcurrently() {
    // Arrange
    setup();
    // Create accounts for the snapshot including an account with the local id.
    accounts = new ArrayList<>(AccountFixture.newAccounts(localId, 10, 10).values());
    when(snapshot.all()).thenReturn(accounts);
    when(network.register(any(ValidatorEngine.class), eq("validator"))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

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
    Assertions.assertTrue(txConduit.hasSent(validatedTransaction.id()));
    try {
      Assertions.assertEquals(txConduit.allEntities().size(), 1);
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }
  }


  @Test
  public void testReceiveNoTransactionNorBlock() {
    // Arrange
    setup();
    // Create accounts for the snapshot including an account with the local id.
    accounts = new ArrayList<>(AccountFixture.newAccounts(localId, 10, 10).values());
    when(snapshot.all()).thenReturn(accounts);
    when(network.register(any(ValidatorEngine.class), eq("validator"))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    // An entity which is neither block nor transaction.
    Signature signature = SignatureFixture.newSignatureFixture();
    try {
      engine.process(signature);
      Assertions.fail("Should have thrown an exception");
    } catch (IllegalArgumentException ex) {
      Assertions.assertEquals(ex.getMessage(), "entity is neither a block nor a transaction");
    }
    Assertions.assertFalse(txConduit.hasSent(signature.id()));
  }
*/
}
