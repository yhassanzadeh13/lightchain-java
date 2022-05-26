package protocol.engines;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import protocol.Engine;
import protocol.Parameters;
import protocol.assigner.LightChainValidatorAssigner;
import protocol.block.BlockValidator;
import state.Snapshot;
import state.State;
import storage.Blocks;
import storage.Transactions;
import unittest.fixtures.*;

/**
 * Encapsulates tests for the proposer engine.
 */
public class ProposerEngineTest {

  private Identifier localId;
  private PrivateKey localPrivateKey;
  private Local local;
  private ArrayList<Account> accounts;
  private Block block;

  /**
   * Initialize non-mocked components.
   */
  @BeforeEach
  void setup() {
    localId = IdentifierFixture.newIdentifier();
    localPrivateKey = KeyGenFixture.newKeyGen().getPrivateKey();
    local = new Local(localId, localPrivateKey);
    accounts = AccountFixture.newAccounts(11);
    block = BlockFixture.newBlock(Parameters.MIN_TRANSACTIONS_NUM + 1);
  }

  /**
   * Evaluates that when a new block arrives at the proposer engine, it creates a valid block and sends it to its
   * assigners.
   */
  @Test
  public void blockValidationTest() {

    Assignment assignment = mock(Assignment.class);
    LightChainValidatorAssigner assigner = mock(LightChainValidatorAssigner.class);
    Transactions pendingTransactions = mock(Transactions.class);
    Blocks blocks = mock(Blocks.class);
    Snapshot snapshot = mock(Snapshot.class);
    State state = mock(State.class);
    mockAssignment(assignment, assigner, pendingTransactions, blocks, snapshot, state);

    NetworkAdapter networkAdapter = mock(NetworkAdapter.class);
    MockConduit proposedCon = new MockConduit(Channels.ProposedBlocks, networkAdapter);
    MockConduit validatedCon = new MockConduit(Channels.ValidatedBlocks, networkAdapter);

    Network network = mock(Network.class);
    when(network.register(any(Engine.class), eq(Channels.ProposedBlocks))).thenReturn(proposedCon);
    when(network.register(any(Engine.class), eq(Channels.ValidatedBlocks))).thenReturn(validatedCon);

    // action
    ProposerEngine proposerEngine = new ProposerEngine(blocks, pendingTransactions, state, local, network, assigner);
    proposerEngine.onNewValidatedBlock(block.getHeight(), block.id());
    BlockValidator blockValidator = new BlockValidator(state);

    // verification
    Assertions.assertTrue(blockValidator.isCorrect(proposerEngine.newB));
  }

  /**
   * Evaluates that when a new block without enough validated transactions arrives at the proposer engine,
   * it keeps waiting till it finds enough transactions in its pending transaction database,
   * and then it creates a valid block and sends it to its assigners.
   */
  @Test
  public void notEnoughValidatedTransactions() throws InterruptedException, LightChainNetworkingException {

    Assignment assignment = mock(Assignment.class);
    LightChainValidatorAssigner assigner = mock(LightChainValidatorAssigner.class);
    Transactions pendingTransactions = mock(Transactions.class);
    AtomicInteger transactionsCounter = new AtomicInteger(Parameters.MIN_TRANSACTIONS_NUM - 1);
    Blocks blocks = mock(Blocks.class);
    Snapshot snapshot = mock(Snapshot.class);
    State state = mock(State.class);
    mockAssignment(assignment, assigner, pendingTransactions, blocks, snapshot, state);
    Conduit proposedCon = mock(Conduit.class);
    Conduit validatedCon = mock(Conduit.class);
    Network network = mock(Network.class);
    when(network.register(any(Engine.class), eq(Channels.ProposedBlocks))).thenReturn(proposedCon);
    when(network.register(any(Engine.class), eq(Channels.ValidatedBlocks))).thenReturn(validatedCon);
    ProposerEngine proposerEngine = new ProposerEngine(blocks,
        pendingTransactions,
        state,
        local,
        network,
        assigner);
    // Verification.
    AtomicBoolean proposerWaiting = new AtomicBoolean(true);
    Thread proposerThread = new Thread(() -> {
      proposerEngine.onNewValidatedBlock(block.getHeight(), block.id());
      proposerWaiting.set(false);
    });
    proposerThread.start(); // start proposer thread
    // checks that proposer is waiting
    verify(proposedCon, times(0)).unicast(any(Block.class), any(Identifier.class));
    Assertions.assertTrue(proposerWaiting.get()); // proposer should be waiting
    Thread ingestThread = new Thread(() -> {
      // simulating adding a transaction to pendingTransactions
      when(pendingTransactions.size()).thenReturn(transactionsCounter.incrementAndGet());
    });
    ingestThread.start(); // start ingest thread
    proposerThread.join(); // wait for proposer to finish
    ingestThread.join(); // wait for ingest to finish
    BlockValidator blockValidator = new BlockValidator(state);
    Assertions.assertTrue(blockValidator.isCorrect(proposerEngine.newB));
    verify(proposedCon, times(Parameters.VALIDATOR_THRESHOLD)).unicast(any(Block.class), any(Identifier.class));
  }

  /**
   * Evaluates that when a new block is received while it is pending
   * for its previously proposed block to get validated. It should throw an IllegalStateException.
   */
  @Test
  public void newValidatedBlockWhilePendingValidation() {
    Assignment assignment = mock(Assignment.class);
    LightChainValidatorAssigner assigner = mock(LightChainValidatorAssigner.class);
    Transactions pendingTransactions = mock(Transactions.class);
    Blocks blocks = mock(Blocks.class);
    Snapshot snapshot = mock(Snapshot.class);
    State state = mock(State.class);
    mockAssignment(assignment, assigner, pendingTransactions, blocks, snapshot, state);
    NetworkAdapter networkAdapter = mock(NetworkAdapter.class);
    MockConduit proposedCon = new MockConduit(Channels.ProposedBlocks, networkAdapter);
    MockConduit validatedCon = new MockConduit(Channels.ValidatedBlocks, networkAdapter);

    Network network = mock(Network.class);
    when(network.register(any(Engine.class), eq(Channels.ProposedBlocks))).thenReturn(proposedCon);
    when(network.register(any(Engine.class), eq(Channels.ValidatedBlocks))).thenReturn(validatedCon);

    // Verification.
    ProposerEngine proposerEngine = new ProposerEngine(blocks, pendingTransactions, state, local, network, assigner);
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

  /**
   * Evaluates that when a new block is received but the block cannot be found on its storage.
   * It should throw an IllegalStateException.
   */
  @Test
  public void blockNotInDatabase() {

    // Initialize mocked components.
    LightChainValidatorAssigner assigner = mock(LightChainValidatorAssigner.class);
    Transactions pendingTransactions = mock(Transactions.class);
    State state = mock(State.class);
    Network network = mock(Network.class);

    Blocks blocks = mock(Blocks.class);
    when(blocks.atHeight(block.getHeight())).thenReturn(BlockFixture.newBlock()); // another block

    // Verification.
    ProposerEngine proposerEngine = new ProposerEngine(blocks, pendingTransactions, state, local, network, assigner);
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      proposerEngine.onNewValidatedBlock(block.getHeight(), block.id());
    });
  }

  /**
   * Evaluates that when enough block approvals are received,
   * a validated block is created and sent to the network (including itself).
   */
  @Test
  public void enoughBlockApproval() throws LightChainNetworkingException {
    Assignment assignment = mock(Assignment.class);
    LightChainValidatorAssigner assigner = mock(LightChainValidatorAssigner.class);
    Transactions pendingTransactions = mock(Transactions.class);
    Blocks blocks = mock(Blocks.class);
    Snapshot snapshot = mock(Snapshot.class);
    State state = mock(State.class);

    mockAssignment(assignment, assigner, pendingTransactions, blocks, snapshot, state);

    ConcurrentMap<Identifier, String> idToAddressMap = new ConcurrentHashMap<>();
    idToAddressMap.put(IdentifierFixture.newIdentifier(), Channels.ValidatedBlocks);

    Conduit validatedCon = mock(Conduit.class);
    doNothing().when(validatedCon).unicast(any(Block.class), any(Identifier.class));

    Conduit proposedCon = mock(Conduit.class);
    doNothing().when(validatedCon).unicast(any(Block.class), any(Identifier.class));

    P2pNetwork network = mock(P2pNetwork.class);
    when(network.getIdToAddressMap()).thenReturn(idToAddressMap);
    when(network.register(any(Engine.class), eq(Channels.ProposedBlocks))).thenReturn(proposedCon);
    when(network.register(any(Engine.class), eq(Channels.ValidatedBlocks))).thenReturn(validatedCon);

    // Verification.
    ProposerEngine proposerEngine = new ProposerEngine(blocks, pendingTransactions, state, local, network, assigner);
    proposerEngine.onNewValidatedBlock(block.getHeight(), block.id());
    for (int i = 0; i < Parameters.VALIDATOR_THRESHOLD; i++) {
      BlockApproval blockApproval = new BlockApproval(SignatureFixture.newSignatureFixture(), block.id());
      proposerEngine.process(blockApproval);
    }
    verify(validatedCon, times(1)).unicast(any(Block.class), any(Identifier.class));
  }

  /**
   * Evaluates that when enough block approvals are received concurrently,
   * a validated block is created and sent to the network (including itself).
   */
  @Test
  public void enoughBlockApprovalConcurrently() throws LightChainNetworkingException, InterruptedException {
    // Initialize mocked components.
    Assignment assignment = mock(Assignment.class);
    LightChainValidatorAssigner assigner = mock(LightChainValidatorAssigner.class);
    Transactions pendingTransactions = mock(Transactions.class);
    Blocks blocks = mock(Blocks.class);
    Snapshot snapshot = mock(Snapshot.class);
    State state = mock(State.class);

    mockAssignment(assignment, assigner, pendingTransactions, blocks, snapshot, state);

    ConcurrentMap<Identifier, String> idToAddressMap = new ConcurrentHashMap<>();
    idToAddressMap.put(IdentifierFixture.newIdentifier(), Channels.ValidatedBlocks);

    Conduit validatedCon = mock(Conduit.class);
    doNothing().when(validatedCon).unicast(any(Block.class), any(Identifier.class));

    Conduit proposedCon = mock(Conduit.class);
    doNothing().when(validatedCon).unicast(any(Block.class), any(Identifier.class));

    P2pNetwork network = mock(P2pNetwork.class);
    when(network.getIdToAddressMap()).thenReturn(idToAddressMap);
    when(network.register(any(Engine.class), eq(Channels.ProposedBlocks))).thenReturn(proposedCon);
    when(network.register(any(Engine.class), eq(Channels.ValidatedBlocks))).thenReturn(validatedCon);

    // Verification.
    ProposerEngine proposerEngine = new ProposerEngine(blocks, pendingTransactions, state, local, network, assigner);
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

  private void mockAssignment(Assignment assignment, LightChainValidatorAssigner assigner,
                              Transactions pendingTransactions, Blocks blocks, Snapshot snapshot, State state) {

    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all identifiers
    when(assignment.has(local.myId())).thenReturn(true);
    when(assignment.all()).thenReturn(IdentifierFixture.newIdentifiers(Parameters.VALIDATOR_THRESHOLD));
    when(assigner.assign(any(Identifier.class), any(Snapshot.class), any(short.class))).thenReturn(assignment);
    when(pendingTransactions.size()).thenReturn(Parameters.MIN_TRANSACTIONS_NUM + 1);
    when(pendingTransactions.all()).thenReturn(new ArrayList<>(Arrays.asList(block.getTransactions())));
    when(blocks.atHeight(block.getHeight())).thenReturn(block); // block to be proposed
    when(snapshot.all()).thenReturn(accounts);
    when(snapshot.getAccount(localId)).thenReturn(accounts.get(0));
    when(state.atBlockId(block.id())).thenReturn(snapshot);
  }

}