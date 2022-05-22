package protocol.engines;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import model.Entity;
import model.crypto.PrivateKey;
import model.crypto.Signature;
import model.exceptions.LightChainDistributedStorageException;
import model.lightchain.Account;
import model.lightchain.Block;
import model.lightchain.Identifier;
import model.lightchain.Transaction;
import model.local.Local;
import network.Channels;
import network.Network;
import network.NetworkAdapter;
import networking.MockConduit;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import protocol.Parameters;
import state.Snapshot;
import state.State;
import storage.Identifiers;
import unittest.fixtures.*;

/**
 * Encapsulates tests for validator engine.
 */
public class ValidatorEngineTest {
  // a single individual test function for each of these scenarios:
  // Note: except when explicitly mentioned, always assume the input block or transaction is assigned to this node
  // i.e., mock the assigner for that.
  //++ 1. Happy path of receiving a valid single block.
  //++ 2. Happy path of receiving two valid blocks sequentially.
  //++ 3. Happy path of receiving two valid blocks concurrently.
  //++ 4. Happy path of receiving two duplicate blocks sequentially (the second duplicate block should be discarded).
  //++ 5. Happy path of receiving two duplicate blocks concurrently (the second duplicate block should be discarded).
  // TODO: is it for ingest 6. Happy path of receiving a valid block with shared transactions in pendingTx.
  // TODO: isn't 7 same with 5? 7. Happy path of receiving an already validated block,
  //    second block should be discarded right away.
  //++ 8.  Unhappy path of receiving a transaction and block (sequentially) that is not assigned to this node.
  //++ 9.  Unhappy path of receiving a transaction and block (concurrently) that is not assigned to this node.
  //++ 10. Happy path of receiving a valid transaction.
  //++ 11. Happy path of receiving two valid transactions sequentially.
  //++ 12. Happy path of receiving two valid transactions concurrently.
  //++ 13. Happy path of receiving a duplicate pair of valid transactions sequentially.
  //+ 14. Happy path of receiving a duplicate pair of valid transactions concurrently.
  //+ TODO: isn't 15 same with 13? 15. Happy path of receiving a transaction that already been validated
  // (second transaction should be discarded).
  //++ 16. Unhappy path of receiving an entity that is neither a block nor a transaction.
  // TODO: is it for ingest 17. Happy path of receiving a transaction and a block concurrently
  //  (block does not contain that transaction).
  // TODO: is it for ingest 18. Happy path of receiving a transaction and a block concurrently
  //  (block does contain the transaction).
  //++ 19. Unhappy path of receiving an invalid transaction (one test per each validation criteria, e.g., correctness,
  //     soundness, etc). Invalid transaction should be discarded without sending back a signature to its sender.
  // 19. Unhappy path of receiving an invalid block (one test per each validation criteria, e.g., correctness,
  //     soundness, etc). Invalid block should be discarded without sending back a signature to its proposer.
  private static final Random random = new Random();
  private ValidatorEngine engine;


  /**
   * Initialize the test. TODO: fix this.
   */
  @Test
  public void testReceiveOneValidBlock() throws LightChainDistributedStorageException {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit blockConduit = new MockConduit(Channels.ProposedBlocks, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];

    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);

    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifiers seenEntities = mock(Identifiers.class);

    Identifier proposer = accounts2.get(propInd).getIdentifier();
    Block block = BlockFixture.newBlock(proposer, block2.id(), block2.getHeight() + 1, snapshot2.all());
    engine = new ValidatorEngine(network, local, state, seenEntities);

    for (Transaction transaction : block.getTransactions()) {
      when(state.atBlockId(transaction.getReferenceBlockId())
          .getAccount(transaction.getSender())
          .getPublicKey().verifySignature(transaction, transaction.getSignature()))
          .thenReturn(true);
    }
    when(state.atBlockId(block.getPreviousBlockId())
        .getAccount(block.getProposer())
        .getPublicKey()
        .verifySignature(block, block.getSignature())).thenReturn(true);

    try {
      engine.process(block);
    } catch (IllegalArgumentException ex) {
      Assertions.fail(ex);
    }

    try {
      for (Entity e : blockConduit.allEntities()) {
        Assertions.assertTrue(blockConduit.hasSent(e.id()));
      }
      Assertions.assertNotEquals(0, blockConduit.allEntities().size());
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }
  }


  @Test
  public void testReceiveTwoValidBlocksSequentially() {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit blockConduit = new MockConduit(Channels.ProposedBlocks, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];

    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);

    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifiers seenEntities = mock(Identifiers.class);
    Identifier proposer = accounts2.get(propInd).getIdentifier();
    Block[] blocks = new Block[2];
    blocks[0] = BlockFixture.newBlock(proposer, block2.id(), block2.getHeight() + 1, snapshot2.all());
    blocks[1] = BlockFixture.newBlock(proposer, block2.id(), block2.getHeight() + 1, snapshot2.all());

    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);
    for (int i = 0; i < 2; i++) {
      when(state.atBlockId(blocks[i].getPreviousBlockId())
          .getAccount(blocks[i].getProposer())
          .getPublicKey()
          .verifySignature(blocks[i], blocks[i].getSignature())).thenReturn(true);
      try {
        engine.process(blocks[i]);
      } catch (IllegalArgumentException ex) {
        Assertions.fail(ex);
      }
    }

    try {
      for (Entity e : blockConduit.allEntities()) {
        Assertions.assertTrue(blockConduit.hasSent(e.id()));
      }
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testReceiveTwoValidBlocksConcurrently() {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit blockConduit = new MockConduit(Channels.ProposedBlocks, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];

    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);

    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifiers seenEntities = mock(Identifiers.class);
    Identifier proposer = accounts2.get(propInd).getIdentifier();
    Block[] blocks = new Block[2];
    blocks[0] = BlockFixture.newBlock(proposer, block2.id(), block2.getHeight() + 1, snapshot2.all());
    blocks[1] = BlockFixture.newBlock(proposer, block2.id(), block2.getHeight() + 1, snapshot2.all());

    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    for (int i = 0; i < 2; i++) {
      when(state.atBlockId(blocks[i].getPreviousBlockId())
          .getAccount(blocks[i].getProposer())
          .getPublicKey()
          .verifySignature(blocks[i], blocks[i].getSignature())).thenReturn(true);
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
          engine.process(blocks[finalI]);
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
    } catch (InterruptedException ex) {
      ex.printStackTrace();
      Assertions.fail(ex);
    }
    Assertions.assertEquals(0, threadErrorCount.get());

    try {
      for (Entity e : blockConduit.allEntities()) {
        Assertions.assertTrue(blockConduit.hasSent(e.id()));
      }
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testReceiveDuplicateBlocksSequentially() {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit blockConduit = new MockConduit(Channels.ProposedBlocks, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];

    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);

    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifiers seenEntities = mock(Identifiers.class);

    Identifier proposer = accounts2.get(propInd).getIdentifier();

    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    Block[] blocks = new Block[2];
    Block b = BlockFixture.newBlock(proposer, block2.id(), block2.getHeight() + 1, snapshot2.all());
    blocks[0] = b;
    blocks[1] = b;
    when(state.atBlockId(blocks[0].getPreviousBlockId())
        .getAccount(blocks[0].getProposer())
        .getPublicKey()
        .verifySignature(blocks[0], blocks[0].getSignature())).thenReturn(true);

    final boolean[] called = {false};
    when(seenEntities.add(blocks[0].id())).thenAnswer(new Answer() {
      public Object answer(InvocationOnMock invocMock) {
        called[0] = true;
        return called[0];
      }
    });
    when(seenEntities.has(blocks[0].id())).thenAnswer(new Answer() {
      public Object answer(InvocationOnMock invocMock) {
        return called[0];
      }
    });

    // Act
    for (int i = 0; i < 2; i++) {
      engine.process(blocks[i]);
    }
    verify(seenEntities, times(1)).add(blocks[0].id());

    try {
      for (Entity e : blockConduit.allEntities()) {
        Assertions.assertTrue(blockConduit.hasSent(e.id()));
      }
      Assertions.assertTrue(blockConduit.allEntities().size() == 1);
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testReceiveDuplicateBlocksConcurrently() {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit blockConduit = new MockConduit(Channels.ProposedBlocks, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];

    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);

    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifiers seenEntities = mock(Identifiers.class);
    Identifier proposer = accounts2.get(propInd).getIdentifier();
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    Block[] blocks = new Block[2];
    Block b = BlockFixture.newBlock(proposer, block2.id(), block2.getHeight() + 1, snapshot2.all());
    blocks[0] = b;
    blocks[1] = b;
    when(state.atBlockId(blocks[0].getPreviousBlockId())
        .getAccount(blocks[0].getProposer())
        .getPublicKey()
        .verifySignature(blocks[0], blocks[0].getSignature())).thenReturn(true);

    final boolean[] called = {false};
    when(seenEntities.add(blocks[0].id())).thenAnswer(new Answer() {
      public Object answer(InvocationOnMock invocMock) {
        called[0] = true;
        return called[0];
      }
    });
    when(seenEntities.has(blocks[0].id())).thenAnswer(new Answer() {
      public Object answer(InvocationOnMock invocMock) {
        return called[0];
      }
    });

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
          engine.process(blocks[finalI]);
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
    } catch (InterruptedException ex) {
      ex.printStackTrace();
      Assertions.fail(ex);
    }
    Assertions.assertEquals(0, threadErrorCount.get());

    verify(seenEntities, times(1)).add(blocks[0].id());

    try {
      for (Entity e : blockConduit.allEntities()) {
        Assertions.assertTrue(blockConduit.hasSent(e.id()));
      }
      Assertions.assertTrue(blockConduit.allEntities().size() == 1);
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testReceiveEntityNotAssignedToThisNodeConcurrently() {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit blockConduit = new MockConduit(Channels.ProposedBlocks, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);
    MockConduit txConduit = new MockConduit(Channels.ProposedTransactions, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];
    ArrayList<Account> accountsNoCurrent = a[2];


    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);

    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifiers seenEntities = mock(Identifiers.class);

    when(snapshot2.all()).thenReturn(accountsNoCurrent);
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);
    Identifier proposer = accounts2.get(propInd).getIdentifier();
    Block b = BlockFixture.newBlock(proposer, block2.id(), block2.getHeight() + 1, snapshot2.all());

    when(state.atBlockId(b.getPreviousBlockId())
        .getAccount(b.getProposer())
        .getPublicKey()
        .verifySignature(b, b.getSignature())).thenReturn(true);

    Identifier signerId = snapshot2.all().get(random.nextInt(accountsNoCurrent.size())).getIdentifier();
    Transaction tx = TransactionFixture.newTransaction(
        block2.id(),
        snapshot2.all().get(0).getIdentifier(),
        snapshot2.all().get(1).getIdentifier(),
        signerId);
    when(state.atBlockId(tx.getReferenceBlockId()).getAccount(tx.getSender()).getPublicKey()
        .verifySignature(tx, tx.getSignature())).thenReturn(true);
    snapshot2.all().get(0).setBalance(tx.getAmount() * 10 + 1);

    // Act
    /// Create two threads that will process the transactions concurrently.
    int concurrencyDegree = 2;
    AtomicInteger threadErrorCount = new AtomicInteger();
    CountDownLatch done = new CountDownLatch(1);
    Thread[] validationThreads = new Thread[2];
    validationThreads[0] = new Thread(() -> {
      try {
        engine.process(b);
      } catch (IllegalArgumentException ex) {
        threadErrorCount.incrementAndGet();
      }
      done.countDown();
    });
    validationThreads[1] = new Thread(() -> {
      try {
        engine.process(tx);
      } catch (IllegalArgumentException ex) {
        threadErrorCount.incrementAndGet();
      }
      done.countDown();
    });

    ///  Run Threads
    for (Thread t : validationThreads) {
      t.start();
    }

    /// Assert done on time and got no errors
    try {
      boolean doneOnTime = done.await(10, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOnTime);
    } catch (InterruptedException ex) {
      ex.printStackTrace();
      Assertions.fail(ex);
    }
    Assertions.assertEquals(0, threadErrorCount.get());

    verify(seenEntities, never()).add(any());

    try {
      Assertions.assertTrue(blockConduit.allEntities().size() == 0);
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testReceiveEntityNotAssignedToThisNodeSequentially() {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit blockConduit = new MockConduit(Channels.ProposedBlocks, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);
    MockConduit txConduit = new MockConduit(Channels.ProposedTransactions, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];
    ArrayList<Account> accountsNoCurrent = a[2];
    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);

    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifiers seenEntities = mock(Identifiers.class);

    when(snapshot2.all()).thenReturn(accountsNoCurrent);
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);
    Identifier proposer = accounts2.get(propInd).getIdentifier();
    Block b = BlockFixture.newBlock(proposer, block2.id(), block2.getHeight() + 1, snapshot2.all());

    when(state.atBlockId(b.getPreviousBlockId())
        .getAccount(b.getProposer())
        .getPublicKey()
        .verifySignature(b, b.getSignature())).thenReturn(true);

    Identifier signerId = snapshot2.all().get(random.nextInt(accountsNoCurrent.size())).getIdentifier();
    Transaction tx = TransactionFixture.newTransaction(
        block2.id(),
        snapshot2.all().get(0).getIdentifier(),
        snapshot2.all().get(1).getIdentifier(),
        signerId);
    when(state.atBlockId(tx.getReferenceBlockId()).getAccount(tx.getSender()).getPublicKey()
        .verifySignature(tx, tx.getSignature())).thenReturn(true);
    snapshot2.all().get(0).setBalance(tx.getAmount() * 10 + 1);

    try {
      engine.process(b);
      engine.process(tx);
    } catch (IllegalArgumentException ex) {
      ex.printStackTrace();
      Assertions.fail();
    }

    verify(seenEntities, never()).add(any());
    try {
      Assertions.assertEquals(0, blockConduit.allEntities().size());
      Assertions.assertEquals(0, txConduit.allEntities().size());
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testReceiveBlockNotCorrect_InvalidPreviousBlockSnapshot() {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit blockConduit = new MockConduit(Channels.ProposedBlocks, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];

    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);

    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifiers seenEntities = mock(Identifiers.class);

    Identifier proposer = accounts2.get(propInd).getIdentifier();
    Identifier newPreviousBlockId = IdentifierFixture.newIdentifier();
    while (accounts2.contains(newPreviousBlockId)) {
      newPreviousBlockId = IdentifierFixture.newIdentifier();
    }
    Block block = BlockFixture.newBlock(proposer, newPreviousBlockId, block2.getHeight() + 1, snapshot2.all());
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    try {
      engine.process(block);
      Assertions.assertEquals(0, blockConduit.allEntities().size());
    } catch (Exception ex) {
      ex.printStackTrace();
      Assertions.fail(ex);
    }

    verify(seenEntities, never()).add(any());
  }

  @Test
  public void testReceiveBlockNotCorrect_InvalidProposer() {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit blockConduit = new MockConduit(Channels.ProposedBlocks, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];

    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);

    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifiers seenEntities = mock(Identifiers.class);
    Identifier invalidProposer = IdentifierFixture.newIdentifier();
    Block block = BlockFixture.newBlock(invalidProposer, block2.id(), block2.getHeight() + 1, snapshot2.all());

    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    try {
      engine.process(block);
      Assertions.assertEquals(0, blockConduit.allEntities().size());
    } catch (Exception ex) {
      ex.printStackTrace();
      Assertions.fail(ex);
    }

    verify(seenEntities, never()).add(any());
  }

  @Test
  public void testReceiveBlockNotCorrect_ValidatedTransactionBelowMinimum() {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit blockConduit = new MockConduit(Channels.ProposedBlocks, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];

    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);

    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifiers seenEntities = mock(Identifiers.class);
    Block block = BlockFixture.newBlock(accounts2.get(propInd).getIdentifier()
        , block2.id()
        , block2.getHeight() + 1
        , snapshot2.all()
        , Parameters.MIN_TRANSACTIONS_NUM - 1);
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);
    when(state.atBlockId(block.getPreviousBlockId())
        .getAccount(block.getProposer())
        .getPublicKey()
        .verifySignature(block, block.getSignature())).thenReturn(true);

    try {
      engine.process(block);
      Assertions.assertEquals(0, blockConduit.allEntities().size());
    } catch (Exception ex) {
      ex.printStackTrace();
      Assertions.fail(ex);
    }

    verify(seenEntities, never()).add(any());
  }

  @Test
  public void testReceiveBlockNotCorrect_ValidatedTransactionAboveMaximum() {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit blockConduit = new MockConduit(Channels.ProposedBlocks, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];

    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);

    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifiers seenEntities = mock(Identifiers.class);
    Block block = BlockFixture.newBlock(accounts2.get(propInd).getIdentifier()
        , block2.id()
        , block2.getHeight() + 1
        , snapshot2.all()
        , Parameters.MAX_TRANSACTIONS_NUM + 1);

    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);
    when(state.atBlockId(block.getPreviousBlockId())
        .getAccount(block.getProposer())
        .getPublicKey()
        .verifySignature(block, block.getSignature())).thenReturn(true);
    try {
      engine.process(block);
      Assertions.assertEquals(0, blockConduit.allEntities().size());
    } catch (Exception ex) {
      ex.printStackTrace();
      Assertions.fail(ex);
    }
    verify(seenEntities, never()).add(any());
  }

  @Test
  public void testReceiveBlockNotConsistent_InvalidPreviousBlockId() {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit blockConduit = new MockConduit(Channels.ProposedBlocks, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];

    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);

    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifiers seenEntities = mock(Identifiers.class);
    Identifier proposer = accounts2.get(propInd).getIdentifier();
    Block block = BlockFixture.newBlock(proposer, block1.id(), block2.getHeight() + 1, snapshot2.all());
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    when(state.atBlockId(block.getPreviousBlockId())
        .getAccount(block.getProposer())
        .getPublicKey()
        .verifySignature(block, block.getSignature())).thenReturn(true);

    try {
      engine.process(block);
      Assertions.assertEquals(0, blockConduit.allEntities().size());
    } catch (Exception ex) {
      ex.printStackTrace();
      Assertions.fail(ex);
    }
    verify(seenEntities, never()).add(any());
  }

  @Test
  public void testReceiveBlockNotAuthenticated() {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit blockConduit = new MockConduit(Channels.ProposedBlocks, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];

    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);

    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifiers seenEntities = mock(Identifiers.class);
    Identifier proposer = accounts2.get(propInd).getIdentifier();
    Block block = BlockFixture.newBlock(proposer, block2.id(), block2.getHeight() + 1, snapshot2.all());
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    when(state.atBlockId(block.getPreviousBlockId())
        .getAccount(block.getProposer())
        .getPublicKey()
        .verifySignature(block, block.getSignature())).thenReturn(false);

    try {
      engine.process(block);
      Assertions.assertEquals(0, blockConduit.allEntities().size());
    } catch (Exception ex) {
      ex.printStackTrace();
      Assertions.fail(ex);
    }
    verify(seenEntities, never()).add(any());

  }

  @Test
  public void testReceiveBlockProposerHasNotEnoughStake() {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit blockConduit = new MockConduit(Channels.ProposedBlocks, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];

    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);
    Identifiers seenEntities = mock(Identifiers.class);
    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() >= Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifier proposer = accounts2.get(propInd).getIdentifier();
    Block block = BlockFixture.newBlock(proposer, block2.id(), block2.getHeight() + 1, snapshot2.all());
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    when(state.atBlockId(block.getPreviousBlockId())
        .getAccount(block.getProposer())
        .getPublicKey()
        .verifySignature(block, block.getSignature())).thenReturn(true);

    try {
      engine.process(block);
      Assertions.assertEquals(0, blockConduit.allEntities().size());
    } catch (Exception ex) {
      ex.printStackTrace();
      Assertions.fail(ex);
    }
    verify(seenEntities, never()).add(any());
  }

  @Test
  public void testReceiveBlockAllTransactionsNotValidated() {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit blockConduit = new MockConduit(Channels.ProposedBlocks, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];

    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);

    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifiers seenEntities = mock(Identifiers.class);
    Identifier proposer = accounts2.get(propInd).getIdentifier();
    Block block = BlockFixture.newBlockUnvalidatedTransaction(proposer, block2.id(), block2.getHeight() + 1, snapshot2.all());
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    for (Transaction transaction : block.getTransactions()) {
      when(state.atBlockId(transaction.getReferenceBlockId())
          .getAccount(transaction.getSender())
          .getPublicKey().verifySignature(transaction, transaction.getSignature()))
          .thenReturn(true);
    }
    when(state.atBlockId(block.getPreviousBlockId())
        .getAccount(block.getProposer())
        .getPublicKey()
        .verifySignature(block, block.getSignature())).thenReturn(true);

    try {
      engine.process(block);
      Assertions.assertEquals(0, blockConduit.allEntities().size());
    } catch (Exception ex) {
      ex.printStackTrace();
      Assertions.fail(ex);
    }
    verify(seenEntities, never()).add(any());
  }

  @Test
  public void testReceiveBlockAllTransactionsNotSound() {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit blockConduit = new MockConduit(Channels.ProposedBlocks, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];

    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);

    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifiers seenEntities = mock(Identifiers.class);
    Identifier proposer = accounts2.get(propInd).getIdentifier();
    Block block = BlockFixture.newBlock(proposer, block2.id(), block2.getHeight() + 1, snapshot2.all());
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    for (Transaction transaction : block.getTransactions()) {
      when(state.atBlockId(transaction.getReferenceBlockId())
          .getAccount(transaction.getSender())
          .getPublicKey().verifySignature(transaction, transaction.getSignature()))
          .thenReturn(true);
    }
    when(state.atBlockId(snapshot2.getAccount(block.getTransactions()[0].getSender()).getLastBlockId())
        .getReferenceBlockHeight()).thenReturn((long) block2.getHeight() * 2 + 10);
    when(state.atBlockId(block.getPreviousBlockId())
        .getAccount(block.getProposer())
        .getPublicKey()
        .verifySignature(block, block.getSignature())).thenReturn(true);

    try {
      engine.process(block);
      Assertions.assertEquals(0, blockConduit.allEntities().size());
    } catch (Exception ex) {
      ex.printStackTrace();
      Assertions.fail(ex);
    }
    verify(seenEntities, never()).add(any());
  }

  @Test
  public void testReceiveBlockDuplicateSender() {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit blockConduit = new MockConduit(Channels.ProposedBlocks, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];

    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);

    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifiers seenEntities = mock(Identifiers.class);
    Identifier proposer = accounts2.get(propInd).getIdentifier();
    Block block = BlockFixture.newBlockDuplicateSender(proposer, block2.id(), block2.getHeight() + 1, snapshot2.all());
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    for (Transaction transaction : block.getTransactions()) {
      when(state.atBlockId(transaction.getReferenceBlockId())
          .getAccount(transaction.getSender())
          .getPublicKey().verifySignature(transaction, transaction.getSignature()))
          .thenReturn(true);
    }

    when(state.atBlockId(block.getPreviousBlockId())
        .getAccount(block.getProposer())
        .getPublicKey()
        .verifySignature(block, block.getSignature())).thenReturn(true);

    try {
      engine.process(block);
      Assertions.assertEquals(0, blockConduit.allEntities().size());
    } catch (Exception ex) {
      ex.printStackTrace();
      Assertions.fail(ex);
    }
    verify(seenEntities, never()).add(any());

  }

  @Test
  public void testReceiveOneValidTransaction() {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit txConduit = new MockConduit(Channels.ProposedTransactions, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];

    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);

    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifiers seenEntities = mock(Identifiers.class);
    // Register the network adapter with the network and create engine.
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    Identifier signerId = snapshot2.all().get(random.nextInt(accounts2.size())).getIdentifier();
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
      ex.printStackTrace();
      Assertions.fail(ex);
    }

    try {
      for (Entity e : txConduit.allEntities()) {
        Assertions.assertTrue(txConduit.hasSent(e.id()));
      }
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }

  }

  @Test
  public void testReceiveTwoValidTransactionsSequentially() {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit txConduit = new MockConduit(Channels.ProposedTransactions, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];

    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);

    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifiers seenEntities = mock(Identifiers.class);
    // Register the network adapter with the network and create engine.
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    Identifier signerId = snapshot2.all().get(random.nextInt(accounts2.size())).getIdentifier();
    ArrayList<Transaction> transactions = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      Transaction transaction = TransactionFixture.newTransaction(
          block2.id(),
          snapshot2.all().get(i).getIdentifier(),
          snapshot2.all().get(i + 1).getIdentifier(),
          signerId);
      transactions.add(transaction);
      when(state.atBlockId(transaction.getReferenceBlockId()).getAccount(transaction.getSender()).getPublicKey()
          .verifySignature(transaction, transaction.getSignature())).thenReturn(true);
      snapshot2.all().get(i).setBalance(transaction.getAmount() * 10 + 1);
    }

    // Act
    for (int i = 0; i < 2; i++) {
      engine.process(transactions.get(i));
    }

    try {
      for (Entity e : txConduit.allEntities()) {
        Assertions.assertTrue(txConduit.hasSent(e.id()));
      }
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testReceiveTwoValidTransactionsConcurrently() {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit txConduit = new MockConduit(Channels.ProposedTransactions, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];

    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);

    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifiers seenEntities = mock(Identifiers.class);
    // Register the network adapter with the network and create engine.
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    Identifier signerId = snapshot2.all().get(random.nextInt(accounts2.size())).getIdentifier();
    ArrayList<Transaction> transactions = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      Transaction transaction = TransactionFixture.newTransaction(
          block2.id(),
          snapshot2.all().get(i).getIdentifier(),
          snapshot2.all().get(i + 1).getIdentifier(),
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
    } catch (InterruptedException ex) {
      ex.printStackTrace();
      Assertions.fail(ex);
    }
    Assertions.assertEquals(0, threadErrorCount.get());

    try {
      for (Entity e : txConduit.allEntities()) {
        Assertions.assertTrue(txConduit.hasSent(e.id()));
      }
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testReceiveDuplicateTransactionsSequentially() {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit txConduit = new MockConduit(Channels.ProposedTransactions, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];

    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);

    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifiers seenEntities = mock(Identifiers.class);
    // Register the network adapter with the network and create engine.
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    Identifier signerId = snapshot2.all().get(random.nextInt(accounts2.size())).getIdentifier();
    ArrayList<Transaction> transactions = new ArrayList<>();
    Transaction transaction = TransactionFixture.newTransaction(
        block2.id(),
        snapshot2.all().get(0).getIdentifier(),
        snapshot2.all().get(1).getIdentifier(),
        signerId);
    when(state.atBlockId(transaction.getReferenceBlockId()).getAccount(transaction.getSender()).getPublicKey()
        .verifySignature(transaction, transaction.getSignature())).thenReturn(true);
    snapshot2.all().get(0).setBalance(transaction.getAmount() * 10 + 1);

    for (int i = 0; i < 2; i++) {
      transactions.add(transaction);
    }

    final boolean[] called = {false};
    when(seenEntities.add(transactions.get(0).id())).thenAnswer(new Answer() {
      public Object answer(InvocationOnMock invocMock) {
        called[0] = true;
        return called[0];
      }
    });
    when(seenEntities.has(transactions.get(0).id())).thenAnswer(new Answer() {
      public Object answer(InvocationOnMock invocMock) {
        return called[0];
      }
    });

    // Act
    for (int i = 0; i < 2; i++) {
      engine.process(transaction);
    }
    verify(seenEntities, times(1)).add(transactions.get(0).id());

    try {
      for (Entity e : txConduit.allEntities()) {
        Assertions.assertTrue(txConduit.hasSent(e.id()));
      }
      Assertions.assertTrue(txConduit.allEntities().size() == 1);
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testReceiveDuplicateTransactionsConcurrently() {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit txConduit = new MockConduit(Channels.ProposedTransactions, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];

    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);

    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifiers seenEntities = mock(Identifiers.class);
    // Register the network adapter with the network and create engine.
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    Identifier signerId = snapshot2.all().get(random.nextInt(accounts2.size())).getIdentifier();
    ArrayList<Transaction> transactions = new ArrayList<>();
    Transaction transaction = TransactionFixture.newTransaction(
        block2.id(),
        snapshot2.all().get(0).getIdentifier(),
        snapshot2.all().get(1).getIdentifier(),
        signerId);
    when(state.atBlockId(transaction.getReferenceBlockId()).getAccount(transaction.getSender()).getPublicKey()
        .verifySignature(transaction, transaction.getSignature())).thenReturn(true);
    snapshot2.all().get(0).setBalance(transaction.getAmount() * 10 + 1);

    for (int i = 0; i < 2; i++) {
      transactions.add(transaction);
    }
    final boolean[] called = {false};
    when(seenEntities.add(transactions.get(0).id())).thenAnswer(new Answer() {
      public Object answer(InvocationOnMock invocMock) {
        called[0] = true;
        return called[0];
      }
    });
    when(seenEntities.has(transactions.get(0).id())).thenAnswer(new Answer() {
      public Object answer(InvocationOnMock invocMock) {
        return called[0];
      }
    });

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
    } catch (InterruptedException ex) {
      ex.printStackTrace();
      Assertions.fail(ex);
    }
    Assertions.assertEquals(0, threadErrorCount.get());

    verify(seenEntities, times(1)).add(transactions.get(0).id());
    try {
      for (Entity e : txConduit.allEntities()) {
        Assertions.assertTrue(txConduit.hasSent(e.id()));
      }
      Assertions.assertTrue(txConduit.allEntities().size() == 1);
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testReceiveNoTransactionNorBlockTxConduit() {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit txConduit = new MockConduit(Channels.ProposedTransactions, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];

    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);

    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifiers seenEntities = mock(Identifiers.class);
    // Register the network adapter with the network and create engine.
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    // An entity which is neither block nor transaction.
    Entity ent = new EntityFixture();
    try {
      engine.process(ent);
      Assertions.fail("Should have thrown an exception");
    } catch (IllegalArgumentException ex) {
      Assertions.assertEquals(ex.getMessage(), "entity is neither a block nor a transaction:" + ent.type());
    }
    Assertions.assertFalse(txConduit.hasSent(ent.id()));
  }

  @Test
  public void testReceiveNoTransactionNorBlockSecondConduit() {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit blockConduit = new MockConduit(Channels.ProposedBlocks, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];

    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);

    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifiers seenEntities = mock(Identifiers.class);
    // Register the network adapter with the network and create engine.
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    // An entity which is neither block nor transaction.
    Signature signature = SignatureFixture.newSignatureFixture();
    try {
      engine.process(signature);
      Assertions.fail("Should have thrown an exception");
    } catch (IllegalArgumentException ex) {
      Assertions.assertEquals(ex.getMessage(), "entity is neither a block nor a transaction:" + signature.type());
    }
    Assertions.assertFalse(blockConduit.hasSent(signature.id()));
  }

  @Test
  public void testReceiveTransactionsSequentially() throws LightChainDistributedStorageException {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit txConduit = new MockConduit(Channels.ProposedTransactions, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];

    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);

    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifiers seenEntities = mock(Identifiers.class);
    // Register the network adapter with the network and create engine.
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    Identifier signerId = snapshot2.all().get(random.nextInt(accounts1.size())).getIdentifier();
    ArrayList<Transaction> transactions = new ArrayList<>();
    Transaction transaction = TransactionFixture.newTransaction(
        block2.id(),
        snapshot2.all().get(0).getIdentifier(),
        snapshot2.all().get(1).getIdentifier(),
        signerId);
    when(state.atBlockId(transaction.getReferenceBlockId()).getAccount(transaction.getSender()).getPublicKey()
        .verifySignature(transaction, transaction.getSignature())).thenReturn(true);
    snapshot2.all().get(0).setBalance(transaction.getAmount() * 10 + 1);

    for (int i = 0; i < 2; i++) {
      transactions.add(transaction);
    }

    final boolean[] called = {false};
    when(seenEntities.add(transactions.get(0).id())).thenAnswer(new Answer() {
      public Object answer(InvocationOnMock invocMock) {
        called[0] = true;
        return called[0];
      }
    });
    when(seenEntities.has(transactions.get(0).id())).thenAnswer(new Answer() {
      public Object answer(InvocationOnMock invocMock) {
        return called[0];
      }
    });

    // Act
    for (int i = 0; i < 2; i++) {
      engine.process(transaction);
    }
    verify(seenEntities, times(1)).add(transactions.get(0).id());

    try {
      for (Entity e : txConduit.allEntities()) {
        Assertions.assertTrue(txConduit.hasSent(e.id()));
      }
      Assertions.assertTrue(txConduit.allEntities().size() == 1);
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testReceiveTransactionNotCorrect_InvalidSender() {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit txConduit = new MockConduit(Channels.ProposedTransactions, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];

    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);

    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifiers seenEntities = mock(Identifiers.class);
    // Register the network adapter with the network and create engine.
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    Identifier invalidSender = IdentifierFixture.newIdentifier();
    while (accounts2.contains(invalidSender)) {
      invalidSender = IdentifierFixture.newIdentifier();
    }
    Identifier signerId = snapshot2.all().get(random.nextInt(accounts2.size())).getIdentifier();
    Transaction transaction = TransactionFixture.newTransaction(
        block2.id(),
        invalidSender,
        snapshot2.all().get(1).getIdentifier(),
        signerId);
    try {
      engine.process(transaction);
    } catch (IllegalArgumentException ex) {
      ex.printStackTrace();
      Assertions.fail("Failed because of another reason that this test was not aiming for.");
    }

    Assertions.assertFalse(txConduit.hasSent(transaction.id()));
    verify(seenEntities, never()).add(any());
  }

  @Test
  public void testReceiveTransactionNotCorrect_InvalidReceiver() {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit txConduit = new MockConduit(Channels.ProposedTransactions, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];

    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);

    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifiers seenEntities = mock(Identifiers.class);
    // Register the network adapter with the network and create engine.
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    Identifier invalidReceiver = IdentifierFixture.newIdentifier();
    while (accounts2.contains(invalidReceiver)) {
      invalidReceiver = IdentifierFixture.newIdentifier();
    }
    Identifier signerId = snapshot2.all().get(random.nextInt(accounts2.size())).getIdentifier();
    Transaction transaction = TransactionFixture.newTransaction(
        block2.id(),
        snapshot2.all().get(0).getIdentifier(),
        invalidReceiver,
        signerId);

    try {
      engine.process(transaction);
    } catch (IllegalArgumentException ex) {
      ex.printStackTrace();
      Assertions.fail("Failed because of another reason that this test was not aiming for.");
    }

    Assertions.assertFalse(txConduit.hasSent(transaction.id()));
    verify(seenEntities, never()).add(any());
  }

  @Test
  public void testReceiveTransactionNotCorrect_NegativeAmount() {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit txConduit = new MockConduit(Channels.ProposedTransactions, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];

    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);

    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifiers seenEntities = mock(Identifiers.class);
    // Register the network adapter with the network and create engine.
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    Identifier signerId = snapshot2.all().get(random.nextInt(accounts2.size())).getIdentifier();
    Transaction transaction = TransactionFixture.newTransaction(
        block2.id(),
        snapshot2.all().get(0).getIdentifier(),
        snapshot2.all().get(1).getIdentifier(),
        signerId,
        -5);

    when(state.atBlockId(transaction.getReferenceBlockId()).getAccount(transaction.getSender()).getPublicKey()
        .verifySignature(transaction, transaction.getSignature())).thenReturn(true);

    try {
      engine.process(transaction);
    } catch (IllegalArgumentException ex) {
      ex.printStackTrace();
      Assertions.fail("Failed because of another reason that this test was not aiming for.");
    }

    Assertions.assertFalse(txConduit.hasSent(transaction.id()));
    verify(seenEntities, never()).add(any());
  }

  @Test
  public void testReceiveTransactionNotSound_LowerHeight() {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit txConduit = new MockConduit(Channels.ProposedTransactions, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];

    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);

    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifiers seenEntities = mock(Identifiers.class);
    // Register the network adapter with the network and create engine.
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    Identifier signerId = snapshot2.all().get(random.nextInt(accounts2.size())).getIdentifier();
    Transaction transaction = TransactionFixture.newTransaction(
        block2.id(),
        snapshot2.all().get(0).getIdentifier(),
        snapshot2.all().get(1).getIdentifier(),
        signerId);

    snapshot2.getAccount(transaction.getSender()).setBalance(transaction.getAmount() * 5 + 5);
    when(state.atBlockId(transaction.getReferenceBlockId()).getAccount(transaction.getSender()).getPublicKey()
        .verifySignature(transaction, transaction.getSignature())).thenReturn(true);

    when(state.atBlockId(snapshot2.getAccount(transaction.getSender()).getLastBlockId()).getReferenceBlockHeight())
        .thenReturn((long) block2.getHeight() + 5);

    try {
      engine.process(transaction);
    } catch (IllegalArgumentException ex) {
      ex.printStackTrace();
      Assertions.fail("Failed because of another reason that this test was not aiming for.");
    }

    Assertions.assertFalse(txConduit.hasSent(transaction.id()));
    verify(seenEntities, never()).add(any());
  }

  @Test
  public void testReceiveTransactionNotAuthenticated() {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit txConduit = new MockConduit(Channels.ProposedTransactions, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];

    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);

    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifiers seenEntities = mock(Identifiers.class);
    // Register the network adapter with the network and create engine.
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    Identifier signerId = snapshot2.all().get(random.nextInt(accounts2.size())).getIdentifier();
    Transaction transaction = TransactionFixture.newTransaction(
        block2.id(),
        snapshot2.all().get(0).getIdentifier(),
        snapshot2.all().get(1).getIdentifier(),
        signerId);

    snapshot2.getAccount(transaction.getSender()).setBalance(transaction.getAmount() * 5 + 5);
    when(state.atBlockId(transaction.getReferenceBlockId()).getAccount(transaction.getSender()).getPublicKey()
        .verifySignature(transaction, transaction.getSignature())).thenReturn(false);

    try {
      engine.process(transaction);
    } catch (IllegalArgumentException ex) {
      ex.printStackTrace();
      Assertions.fail("Failed because of another reason that this test was not aiming for.");
    }

    Assertions.assertFalse(txConduit.hasSent(transaction.id()));
    verify(seenEntities, never()).add(any());
  }

  @Test
  public void testReceiveTransactionNotEnoughBalance() {
    Local local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
    Network network = mock(Network.class);
    MockConduit txConduit = new MockConduit(Channels.ProposedTransactions, mock(NetworkAdapter.class));
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);

    Block genesis = BlockFixture.newBlock();
    Block block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
    Block block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    ArrayList<Account>[] a = AccountFixture.newAccounts(local.myId(), genesis.id(),
        block1.id(), 10, 10);
    ArrayList<Account> accounts1 = a[0];
    ArrayList<Account> accounts2 = a[1];

    /// State and Snapshots
    State state = mock(State.class);
    Snapshot genesisSnapshot = mock(Snapshot.class);
    Snapshot snapshot1 = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);

    when(genesisSnapshot.getReferenceBlockId()).thenReturn(genesis.id());
    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(genesisSnapshot.getReferenceBlockHeight()).thenReturn((long) genesis.getHeight());
    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(genesis.id())).thenReturn(genesisSnapshot);
    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    when(state.last()).thenReturn(snapshot2);

    int propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
    Identifiers seenEntities = mock(Identifiers.class);
    // Register the network adapter with the network and create engine.
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    Identifier signerId = snapshot2.all().get(random.nextInt(accounts2.size())).getIdentifier();
    Transaction transaction = TransactionFixture.newTransaction(
        block2.id(),
        snapshot2.all().get(0).getIdentifier(),
        snapshot2.all().get(1).getIdentifier(),
        signerId);

    snapshot2.getAccount(transaction.getSender()).setBalance(transaction.getAmount() - 5);
    when(state.atBlockId(transaction.getReferenceBlockId()).getAccount(transaction.getSender()).getPublicKey()
        .verifySignature(transaction, transaction.getSignature())).thenReturn(true);

    try {
      engine.process(transaction);
    } catch (IllegalArgumentException ex) {
      ex.printStackTrace();
      Assertions.fail("Failed because of another reason that this test was not aiming for.");
    }

    Assertions.assertFalse(txConduit.hasSent(transaction.id()));
    verify(seenEntities, never()).add(any());
  }
}
