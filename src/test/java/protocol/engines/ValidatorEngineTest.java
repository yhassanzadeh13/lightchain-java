package protocol.engines;

import java.util.ArrayList;
import java.util.Random;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.prometheus.client.Counter;
import metrics.collectors.LightChainCollector;
import metrics.collectors.MetricServer;
import metrics.integration.MetricsTestNet;
import model.Entity;
import model.crypto.PrivateKey;
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
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
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
  private Identifier localId;
  private PrivateKey localPrivateKey;
  private Local local;
  private Network network;
  private MockConduit blockConduit;
  private MockConduit txConduit;
  private Identifiers seenEntities;
  private Block block1;
  private Block block2;
  private State state;
  private Snapshot snapshot1;
  private Snapshot snapshot2;

  private ArrayList<Account> accounts1;
  private ArrayList<Account> accounts2;
  private ArrayList<Account> accountsNoCurrent;
  private int propInd;


  static LightChainCollector collector;
  static Counter incomingBlockCount;
  static Counter incomingTransactionCount;
  static Counter validBlocks;
  static Counter validTransactions;

  @BeforeClass
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
      incomingBlockCount = collector.counter().register("incoming_block_count",
              "consensus", "ValidatorEngine", "Number of blocks received");
      incomingTransactionCount = collector.counter().register("incoming_transaction_count",
              "consensus", "ValidatorEngine", "Number of transactions received");
      validBlocks = collector.counter().register("valid_block_count",
              "consensus", "ValidatorEngine", "Number of blocks that passed validation");
      validTransactions = collector.counter().register("valid_transaction_count",
              "consensus", "ValidatorEngine", "Number of transactions that passed validation");

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

  @AfterClass
  public static void tearDown() {

    while(true);

  }

  /**
   * Initialize the test. TODO: fix this.
   */
  @Before
  public void setup() {
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

    /// State and Snapshots
    state = mock(State.class);
    snapshot1 = mock(Snapshot.class);
    snapshot2 = mock(Snapshot.class);

    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    /// Accounts
    /// Create accounts for the snapshot including an account with the local id.
    ArrayList<Account>[] a = AccountFixture.newAccounts(localId, block1.id(),
            block2.id(), 10, 10);
    accounts1 = a[0];
    accounts2 = a[1];
    accountsNoCurrent = a[2];
    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts2);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts2.get(i));
    }

    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    seenEntities = mock(Identifiers.class);
    when(state.last()).thenReturn(snapshot2);
    propInd = random.nextInt(accounts2.size());
    while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
      propInd = random.nextInt(accounts2.size());
    }
  }

  @Test
  public void testReceiveTwoValidBlocksSequentially() {


    while (true) {

      try {
        Thread.sleep(1000);
      } catch (InterruptedException ex) {
        System.err.println("Thread sleep issue, breaking the loop");
        break;
      }

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
      Identifier proposer = accounts2.get(propInd).getIdentifier();
      Block[] blocks = new Block[2];
      blocks[0] = BlockFixture.newBlock(proposer, block2.id(), block2.getHeight() + 1, snapshot2.all());
      blocks[1] = BlockFixture.newBlock(proposer, block2.id(), block2.getHeight() + 1, snapshot2.all());
      Identifiers seenEntities = mock(Identifiers.class);

      when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);
      engine = new ValidatorEngine(network, local, state, seenEntities, incomingBlockCount,
              incomingTransactionCount, validBlocks, validTransactions);
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


      local = new Local(IdentifierFixture.newIdentifier(), KeyGenFixture.newKeyGen().getPrivateKey());
      network = mock(Network.class);
      MockConduit txConduit = new MockConduit(Channels.ProposedTransactions, mock(NetworkAdapter.class));
      when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);

       genesis = BlockFixture.newBlock();
       block1 = BlockFixture.newBlock(genesis.id(), genesis.getHeight() + 1);
       block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

      a = AccountFixture.newAccounts(local.myId(), genesis.id(),
              block1.id(), 10, 10);
      accounts1 = a[0];
      accounts2 = a[1];

      /// State and Snapshots
       state = mock(State.class);
       genesisSnapshot = mock(Snapshot.class);
       snapshot1 = mock(Snapshot.class);
       snapshot2 = mock(Snapshot.class);

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

       propInd = random.nextInt(accounts2.size());
      while (accounts2.get(propInd).getStake() < Parameters.MINIMUM_STAKE) {
        propInd = random.nextInt(accounts2.size());
      }
       seenEntities = mock(Identifiers.class);
      // Register the network adapter with the network and create engine.
      when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
      engine = new ValidatorEngine(network, local, state, seenEntities, incomingBlockCount,
              incomingTransactionCount, validBlocks, validTransactions);

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
  }

}