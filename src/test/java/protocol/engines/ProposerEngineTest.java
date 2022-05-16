package protocol.engines;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import model.crypto.PrivateKey;
import model.exceptions.LightChainNetworkingException;
import model.lightchain.*;
import model.local.Local;
import network.Channels;
import network.Conduit;
import network.Network;
import network.NetworkAdapter;
import network.p2p.P2pNetwork;
import networking.MockConduit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import protocol.Engine;
import protocol.Parameters;
import protocol.block.BlockValidator;
import state.Snapshot;
import state.State;
import storage.Blocks;
import storage.Identifiers;
import storage.Transactions;
import unittest.fixtures.*;

/**
 * Encapsulates tests for the proposer engine.
 */
public class ProposerEngineTest {

  // TODO: implement following test cases.
  // Mock assigner, state, snapshot, network, and storage components.
  // Create snapshot of 11 staked accounts.
  // 1. When proposer receives a new block and finds that it is the proposer of the next block, it creates a valid block
  // and sends to its assigners (i.e., 10 out of 11 nodes). The test should check that the block proposer engine creates
  // can successfully pass block validation (using a real BlockValidator).
  // 2. Same as case 1, but proposer engine does not have enough validated transactions, hence it keeps waiting till it
  // finds enough transactions in its shared pending transactions storage.
  // 3. Proposer engine throws an illegal state exception if it receives a new validated block while it
  // is pending for its previously proposed block to get validated.
  // 4. Proposer engine throws an illegal argument exception when is notified of a new validated block but cannot find
  // it on its storage (using either height or id).
  // 5. Proposer engine creates a validated block whenever it receives enough block approvals for its proposed block,
  // it also sends the validated block to all nodes including itself on the expected channel.
  // 6. Extend test case 5 for when proposer engine receives all block approvals concurrently. 

  @Test
  public void blockValidationTest() {
    // Initialize non-mocked components.
    Identifier localId = IdentifierFixture.newIdentifier();
    PrivateKey localPrivateKey = KeyGenFixture.newKeyGen().getPrivateKey();
    Local local = new Local(localId, localPrivateKey);
    ArrayList<Account> accounts = AccountFixture.newAccounts(11);
    Block block = BlockFixture.newBlock(Parameters.MIN_TRANSACTIONS_NUM + 1);

    // Initialize mocked components.
    Assignment assignment = mock(Assignment.class);
    Transactions pendingTransactions = mock(Transactions.class);
    Blocks blocks = mock(Blocks.class);
    State state = mock(State.class);
    Snapshot snapshot = mock(Snapshot.class);
    Network network = mock(Network.class);
    NetworkAdapter networkAdapter = mock(NetworkAdapter.class);
    MockConduit proposedCon = new MockConduit(Channels.ProposedBlocks, networkAdapter);
    MockConduit validatedCon = new MockConduit(Channels.ValidatedBlocks, networkAdapter);

    // Initialize mocked returns.
    when(blocks.atHeight(block.getHeight())).thenReturn(block); // block to be proposed
    when(snapshot.all()).thenReturn(accounts);
    when(snapshot.getAccount(localId)).thenReturn(accounts.get(0));
    when(assignment.has(local.myId())).thenReturn(true);
    when(state.atBlockId(block.id())).thenReturn(snapshot);
    when(pendingTransactions.size()).thenReturn(Parameters.MIN_TRANSACTIONS_NUM + 1);
    when(pendingTransactions.all()).thenReturn(new ArrayList<>(Arrays.asList(block.getTransactions())));
    when(network.register(any(Engine.class), eq(Channels.ProposedBlocks))).thenReturn(proposedCon);
    when(network.register(any(Engine.class), eq(Channels.ValidatedBlocks))).thenReturn(validatedCon);

    // Verification.
    ProposerEngine proposerEngine = new ProposerEngine(blocks, pendingTransactions, state, local, network, assignment);
    proposerEngine.onNewValidatedBlock(block.getHeight(), block.id());
    BlockValidator blockValidator = new BlockValidator(state);
    Assertions.assertTrue(blockValidator.isCorrect(proposerEngine.newB));
  }

  @Test
  public void notEnoughValidatedTransactions() throws InterruptedException, LightChainNetworkingException {
    // Initialize non-mocked components.
    Random random = new Random();
    Identifier localId = IdentifierFixture.newIdentifier();
    PrivateKey localPrivateKey = KeyGenFixture.newKeyGen().getPrivateKey();
    Local local = new Local(localId, localPrivateKey);
    ArrayList<Account> accounts = AccountFixture.newAccounts(11);
    Block block = BlockFixture.newBlock(Parameters.MIN_TRANSACTIONS_NUM - 1);

    // Initialize mocked components.
    Assignment assignment = mock(Assignment.class);
    Transactions pendingTransactions = mock(Transactions.class);
    Identifiers transactions = mock(Identifiers.class);
    Identifiers seenEntities = mock(Identifiers.class);
    Blocks blocks = mock(Blocks.class);
    State state = mock(State.class);
    Snapshot snapshot = mock(Snapshot.class);
    Network network = mock(Network.class);
    NetworkAdapter networkAdapter = mock(NetworkAdapter.class);
    Conduit proposedCon = mock(Conduit.class);
    Conduit validatedCon = mock(Conduit.class);

    // Initialize mocked returns.
    when(blocks.atHeight(block.getHeight())).thenReturn(block); // block to be proposed
    when(snapshot.all()).thenReturn(accounts);
    when(snapshot.getAccount(localId)).thenReturn(accounts.get(0));
    when(assignment.has(local.myId())).thenReturn(true);
    when(state.atBlockId(block.id())).thenReturn(snapshot);
    AtomicInteger transactionsCounter = new AtomicInteger(Parameters.MIN_TRANSACTIONS_NUM - 1);
    when(pendingTransactions.size()).thenReturn(transactionsCounter.get());
    when(network.register(any(Engine.class), eq(Channels.ProposedBlocks))).thenReturn(proposedCon);
    when(network.register(any(Engine.class), eq(Channels.ValidatedBlocks))).thenReturn(validatedCon);

    // Transaction(s) to be added
    ArrayList<Transaction> transactionsList = new ArrayList<>();
    ValidatedTransaction validatedTransaction = ValidatedTransactionFixture.newValidatedTransaction();
    transactionsList.add(validatedTransaction);
    doReturn(transactionsList).when(pendingTransactions).all();

    // Verification.
    ProposerEngine proposerEngine = new ProposerEngine(blocks, pendingTransactions, state, local, network, assignment);
    IngestEngine ingestEngine = new IngestEngine(state, blocks, transactions,
            pendingTransactions, seenEntities, assignment);

    AtomicBoolean proposerWaiting = new AtomicBoolean(true);
    Thread proposerThread = new Thread(() -> {
      proposerEngine.onNewValidatedBlock(block.getHeight(), block.id());
      proposerWaiting.set(false);
    });
    Thread ingestThread = new Thread(() -> {
      // simulating adding a transaction to pendingTransactions
      ingestEngine.process(validatedTransaction);
      when(pendingTransactions.size()).thenReturn(transactionsCounter.incrementAndGet());
    });
    proposerThread.start(); // start proposer thread
    Thread.sleep(100); // wait for the proposer thread to start
    // checks that proposer is waiting
    verify(proposedCon, times(0)).unicast(any(Block.class), any(Identifier.class));
    Thread.sleep(100); // wait for verify to be called
    Assertions.assertTrue(proposerWaiting.get()); // proposer should be waiting
    ingestThread.start(); // start ingest thread
    proposerThread.join(); // wait for proposer to finish
    ingestThread.join(); // wait for ingest to finish
    BlockValidator blockValidator = new BlockValidator(state);
    Assertions.assertTrue(blockValidator.isCorrect(proposerEngine.newB));
    verify(proposedCon, times(Parameters.VALIDATOR_THRESHOLD)).unicast(any(Block.class), any(Identifier.class));
  }

  @Test
  public void newValidatedBlockWhilePendingValidation() {
    // Initialize non-mocked components.
    Random random = new Random();
    Identifier localId = IdentifierFixture.newIdentifier();
    PrivateKey localPrivateKey = KeyGenFixture.newKeyGen().getPrivateKey();
    Local local = new Local(localId, localPrivateKey);
    ArrayList<Account> accounts = AccountFixture.newAccounts(11);
    Block block = BlockFixture.newBlock(Parameters.MIN_TRANSACTIONS_NUM + 1);

    // Initialize mocked components.
    Assignment assignment = mock(Assignment.class);
    Transactions pendingTransactions = mock(Transactions.class);
    Blocks blocks = mock(Blocks.class);
    State state = mock(State.class);
    Snapshot snapshot = mock(Snapshot.class);
    Network network = mock(Network.class);
    NetworkAdapter networkAdapter = mock(NetworkAdapter.class);
    MockConduit proposedCon = new MockConduit(Channels.ProposedBlocks, networkAdapter);
    MockConduit validatedCon = new MockConduit(Channels.ValidatedBlocks, networkAdapter);

    // Initialize mocked returns.
    when(blocks.atHeight(block.getHeight())).thenReturn(block); // block to be proposed
    when(snapshot.all()).thenReturn(accounts);
    when(snapshot.getAccount(localId)).thenReturn(accounts.get(0));
    when(assignment.has(local.myId())).thenReturn(true);
    when(state.atBlockId(block.id())).thenReturn(snapshot);
    when(pendingTransactions.size()).thenReturn(Parameters.MIN_TRANSACTIONS_NUM - 1);
    when(pendingTransactions.all()).thenReturn(new ArrayList<>(Arrays.asList(block.getTransactions())));
    when(network.register(any(Engine.class), eq(Channels.ProposedBlocks))).thenReturn(proposedCon);
    when(network.register(any(Engine.class), eq(Channels.ValidatedBlocks))).thenReturn(validatedCon);

    // Verification.
    ProposerEngine proposerEngine = new ProposerEngine(blocks, pendingTransactions, state, local, network, assignment);
    Thread proposerThread = new Thread(() -> {
      try {
        Assertions.assertThrows(IllegalStateException.class, () -> {
          proposerEngine.onNewValidatedBlock(block.getHeight(), block.id());
        });
      } finally {
        // increments the transactions counter for main thread to be finished
        when(pendingTransactions.size()).thenReturn(Parameters.MIN_TRANSACTIONS_NUM + 1);
      }
    });
    proposerThread.start(); // start proposer thread
    proposerEngine.onNewValidatedBlock(block.getHeight(), block.id()); // new validated block while pending validation
  }

  @Test
  public void blockNotInDatabase() {
    // Initialize non-mocked components.
    Identifier localId = IdentifierFixture.newIdentifier();
    PrivateKey localPrivateKey = KeyGenFixture.newKeyGen().getPrivateKey();
    Local local = new Local(localId, localPrivateKey);
    Block block = BlockFixture.newBlock(Parameters.MIN_TRANSACTIONS_NUM + 1);

    // Initialize mocked components.
    Assignment assignment = mock(Assignment.class);
    Transactions pendingTransactions = mock(Transactions.class);
    Blocks blocks = mock(Blocks.class);
    State state = mock(State.class);
    Network network = mock(Network.class);

    // Initialize mocked returns.
    when(blocks.atHeight(block.getHeight())).thenReturn(BlockFixture.newBlock()); // another block

    // Verification.
    ProposerEngine proposerEngine = new ProposerEngine(blocks, pendingTransactions, state, local, network, assignment);
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      proposerEngine.onNewValidatedBlock(block.getHeight(), block.id());
    });
  }

  @Test
  public void enoughBlockApproval() throws LightChainNetworkingException {
    // Initialize non-mocked components.
    Identifier localId = IdentifierFixture.newIdentifier();
    PrivateKey localPrivateKey = KeyGenFixture.newKeyGen().getPrivateKey();
    Local local = new Local(localId, localPrivateKey);
    ArrayList<Account> accounts = AccountFixture.newAccounts(11);
    Block block = BlockFixture.newBlock(Parameters.MIN_TRANSACTIONS_NUM + 1);

    // Initialize mocked components.
    Assignment assignment = mock(Assignment.class);
    Transactions pendingTransactions = mock(Transactions.class);
    Blocks blocks = mock(Blocks.class);
    State state = mock(State.class);
    Snapshot snapshot = mock(Snapshot.class);
    P2pNetwork network = mock(P2pNetwork.class);
    NetworkAdapter networkAdapter = mock(NetworkAdapter.class);
    MockConduit proposedCon = new MockConduit(Channels.ProposedBlocks, networkAdapter);
    Conduit validatedCon = mock(Conduit.class);

    // Initialize mocked returns.
    ConcurrentMap<Identifier, String> idToAddressMap = new ConcurrentHashMap<>();
    idToAddressMap.put(IdentifierFixture.newIdentifier(), Channels.ValidatedBlocks);
    when(network.getIdToAddressMap()).thenReturn(idToAddressMap);
    doNothing().when(validatedCon).unicast(any(Block.class), any(Identifier.class));
    when(blocks.atHeight(block.getHeight())).thenReturn(block); // block to be proposed
    when(snapshot.all()).thenReturn(accounts);
    when(snapshot.getAccount(localId)).thenReturn(accounts.get(0));
    when(assignment.has(local.myId())).thenReturn(true);
    when(state.atBlockId(block.id())).thenReturn(snapshot);
    when(pendingTransactions.size()).thenReturn(Parameters.MIN_TRANSACTIONS_NUM + 1);
    when(pendingTransactions.all()).thenReturn(new ArrayList<>(Arrays.asList(block.getTransactions())));
    when(network.register(any(Engine.class), eq(Channels.ProposedBlocks))).thenReturn(proposedCon);
    when(network.register(any(Engine.class), eq(Channels.ValidatedBlocks))).thenReturn(validatedCon);

    // Verification.
    ProposerEngine proposerEngine = new ProposerEngine(blocks, pendingTransactions, state, local, network, assignment);
    proposerEngine.onNewValidatedBlock(block.getHeight(), block.id());
    BlockValidator blockValidator = new BlockValidator(state);
    for (int i = 0; i < Parameters.VALIDATOR_THRESHOLD; i++) {
      BlockApproval blockApproval = new BlockApproval(SignatureFixture.newSignatureFixture(), block.id());
      proposerEngine.process(blockApproval);
    }
    verify(validatedCon, times(1)).unicast(any(Block.class), any(Identifier.class));
  }

  @Test
  public void enoughBlockApprovalConcurrently() throws LightChainNetworkingException, InterruptedException {
    // Initialize non-mocked components.
    Identifier localId = IdentifierFixture.newIdentifier();
    PrivateKey localPrivateKey = KeyGenFixture.newKeyGen().getPrivateKey();
    Local local = new Local(localId, localPrivateKey);
    ArrayList<Account> accounts = AccountFixture.newAccounts(11);
    Block block = BlockFixture.newBlock(Parameters.MIN_TRANSACTIONS_NUM + 1);

    // Initialize mocked components.
    Assignment assignment = mock(Assignment.class);
    Transactions pendingTransactions = mock(Transactions.class);
    Blocks blocks = mock(Blocks.class);
    State state = mock(State.class);
    Snapshot snapshot = mock(Snapshot.class);
    P2pNetwork network = mock(P2pNetwork.class);
    NetworkAdapter networkAdapter = mock(NetworkAdapter.class);
    MockConduit proposedCon = new MockConduit(Channels.ProposedBlocks, networkAdapter);
    Conduit validatedCon = mock(Conduit.class);

    // Initialize mocked returns.
    ConcurrentMap<Identifier, String> idToAddressMap = new ConcurrentHashMap<>();
    idToAddressMap.put(IdentifierFixture.newIdentifier(), Channels.ValidatedBlocks);
    when(network.getIdToAddressMap()).thenReturn(idToAddressMap);
    doNothing().when(validatedCon).unicast(any(Block.class), any(Identifier.class));
    when(blocks.atHeight(block.getHeight())).thenReturn(block); // block to be proposed
    when(snapshot.all()).thenReturn(accounts);
    when(snapshot.getAccount(localId)).thenReturn(accounts.get(0));
    when(assignment.has(local.myId())).thenReturn(true);
    when(state.atBlockId(block.id())).thenReturn(snapshot);
    when(pendingTransactions.size()).thenReturn(Parameters.MIN_TRANSACTIONS_NUM + 1);
    when(pendingTransactions.all()).thenReturn(new ArrayList<>(Arrays.asList(block.getTransactions())));
    when(network.register(any(Engine.class), eq(Channels.ProposedBlocks))).thenReturn(proposedCon);
    when(network.register(any(Engine.class), eq(Channels.ValidatedBlocks))).thenReturn(validatedCon);

    // Verification.
    ProposerEngine proposerEngine = new ProposerEngine(blocks, pendingTransactions, state, local, network, assignment);
    proposerEngine.onNewValidatedBlock(block.getHeight(), block.id());
    Thread[] threads = new Thread[Parameters.VALIDATOR_THRESHOLD];
    for (int i = 0; i < Parameters.VALIDATOR_THRESHOLD; i++) {
      threads[i] = new Thread(() -> {
        BlockApproval blockApproval = new BlockApproval(SignatureFixture.newSignatureFixture(), block.id());
        proposerEngine.process(blockApproval);
      });
    }
    for (Thread thread : threads) {
      thread.start();
      thread.join();
    }
    verify(validatedCon, times(1)).unicast(any(Block.class), any(Identifier.class));
  }
}
