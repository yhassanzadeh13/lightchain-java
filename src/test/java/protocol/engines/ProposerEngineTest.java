package protocol.engines;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
import network.p2p.P2pNetwork;
import org.apache.commons.compress.utils.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import protocol.Engine;
import protocol.Parameters;
import protocol.assigner.ProposerAssignerInf;
import protocol.assigner.ValidatorAssignerInf;
import protocol.assigner.lightchain.Assigner;
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

  /**
   * Evaluates that when a new block arrives at the proposer engine, it creates a valid block and sends it to its
   * assigners.
   */
  @Test
  public void blockValidationTest() {
    Local local = LocalFixture.newLocal();
    Block currentBlock = BlockFixture.newBlock(Parameters.MIN_TRANSACTIONS_NUM + 1);
    Blocks blocks = mockBlockStorageForBlock(currentBlock);

    State state = mock(State.class);
    Snapshot snapshot = mock(Snapshot.class);
    when(state.atBlockId(any(Identifier.class))).thenReturn(snapshot);
    ArrayList<Identifier> validators = IdentifierFixture.newIdentifiers(Parameters.VALIDATOR_THRESHOLD);

    // mocks this node as the proposer of the next block.
    ProposerAssignerInf proposerAssigner = mockIdAsNextBlockProposer(local.myId(), state, currentBlock);
    ValidatorAssignerInf validatorAssigner = mockValidatorAssigner(validators);

    Transactions pendingTransactions = mockValidatedTransactions(1);

    Network network = mock(Network.class);
    Conduit proposedCon = mock(Conduit.class);
    when(network.register(any(ProposerEngine.class), eq(Channels.ProposedBlocks))).thenReturn(proposedCon);


    BlockValidator validator = new BlockValidator(state);
    CountDownLatch blockSentForValidation = new CountDownLatch(Parameters.VALIDATOR_THRESHOLD);
    try {
      doAnswer(invocationOnMock -> {
        Block block = invocationOnMock.getArgument(0, Block.class);
        validator.isCorrect(block);

        blockSentForValidation.countDown();
        return null;
      }).when(proposedCon).unicast(any(Block.class), any(Identifier.class));
    } catch (LightChainNetworkingException e) {
      Assertions.fail();
    }


    // action
    ProposerEngine proposerEngine = new ProposerEngine(blocks, pendingTransactions, state, local, network, validatorAssigner, proposerAssigner);
    proposerEngine.onNewValidatedBlock(currentBlock.getHeight(), currentBlock.id());

    try {
      boolean doneOnTime = blockSentForValidation.await(1000, TimeUnit.MILLISECONDS);
      Assertions.assertTrue(doneOnTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }

//    BlockValidator blockValidator = new BlockValidator(state);
//
//    // verification
//    Assertions.assertTrue(blockValidator.isCorrect(proposerEngine.));
  }

  public Blocks mockBlockStorageForBlock(Block block) {
    Blocks blocks = mock(Blocks.class);
    when(blocks.has(block.id())).thenReturn(true);
    when(blocks.atHeight(block.getHeight())).thenReturn(block); // block to be proposed
    return blocks;
  }

  public ProposerAssignerInf mockIdAsNextBlockProposer(Identifier id, State state, Block currentBlock) {
    ProposerAssignerInf assigner = mock(ProposerAssignerInf.class);
    Snapshot snapshot = mock(Snapshot.class);
    when(state.atBlockId(currentBlock.id())).thenReturn(snapshot);
    when(assigner.nextBlockProposer(currentBlock.id(), snapshot)).thenReturn(id);
    return assigner;
  }

  public ValidatorAssignerInf mockValidatorAssigner(ArrayList<Identifier> validators) {
    ValidatorAssignerInf assigner = mock(ValidatorAssignerInf.class);
    Assignment assignment = new Assignment();
    for (Identifier validator : validators) {
      assignment.add(validator);
    }
    when(assigner.getValidatorsAtSnapshot(any(Identifier.class), any(Snapshot.class))).thenReturn(assignment);
    return assigner;
  }

  /**
   * Generates validated transaction fixtures and mock a Transactions storage with it.
   *
   * @param count total validated transactions to be created.
   * @return a Transactions storage which mocked with validated transactions.
   */
  public Transactions mockValidatedTransactions(int count) {
    ValidatedTransaction[] transactions = ValidatedTransactionFixture.newValidatedTransactions(count);
    Transactions transactionsStorage = mock(Transactions.class);
    when(transactionsStorage.size()).thenReturn(transactions.length);
    when(transactionsStorage.all()).thenReturn(Lists.newArrayList(Arrays.stream(transactions).iterator()));
    return transactionsStorage;
  }

//  private Conduit mockProposerConduit(ArrayList<Identifier> validators, Network network) {
//
//
//    try {
//      when(proposedCon.unicast(any(Block.class), any(Identifier.class))).thenAnswer(invocationOnMock -> {
//        // Block block = invocationOnMock.getArgument(0);
//        Identifier validator = invocationOnMock.getArgument(1);
//
//        // block should be sent to the right validators.
//        Assertions.assertTrue(validators.contains(validator));
//
//        return null;
//      });
//    } catch (Exception e) {
//      Assertions.fail("should not have exceptions upon unicasting");
//    }
//
//    return proposedCon;
//  }


  /**
   * Evaluates that when a new block without enough validated transactions arrives at the proposer engine,
   * it keeps waiting till it finds enough transactions in its pending transaction database,
   * and then it creates a valid block and sends it to its assigners.
   */
  @Test
  public void notEnoughValidatedTransactions() throws InterruptedException, LightChainNetworkingException {
    Identifier localId = IdentifierFixture.newIdentifier();
    PrivateKey localPrivateKey = KeyGenFixture.newKeyGen().getPrivateKey();
    Local local = new Local(localId, localPrivateKey);
    ArrayList<Account> accounts = AccountFixture.newAccounts(11);
    Block block = BlockFixture.newBlock(Parameters.MIN_TRANSACTIONS_NUM + 1);
    Transactions pendingTransactions = mock(Transactions.class);
    State state = mock(State.class);

    Conduit proposedCon = mock(Conduit.class);
    Conduit validatedCon = mock(Conduit.class);
    Network network = mock(Network.class);
    mockNetwork(network, proposedCon, validatedCon);
    ProposerEngine proposerEngine = mockProposerEngine(local, accounts, block, network, pendingTransactions, state);
    waitEnoughTransactions(proposerEngine, block, proposedCon, pendingTransactions, state);

    verify(proposedCon, times(Parameters.VALIDATOR_THRESHOLD)).unicast(any(Block.class), any(Identifier.class));
  }

  /**
   * Evaluates that when a new block is received while it is pending
   * for its previously proposed block to get validated. It should throw an IllegalStateException.
   */
  @Test
  public void newValidatedBlockWhilePendingValidation() {
    Identifier localId = IdentifierFixture.newIdentifier();
    PrivateKey localPrivateKey = KeyGenFixture.newKeyGen().getPrivateKey();
    Local local = new Local(localId, localPrivateKey);
    ArrayList<Account> accounts = AccountFixture.newAccounts(11);
    Block block = BlockFixture.newBlock(Parameters.MIN_TRANSACTIONS_NUM + 1);

    State state = mock(State.class);
    Transactions pendingTransactions = mock(Transactions.class);
    Conduit proposedCon = mock(Conduit.class);
    Conduit validatedCon = mock(Conduit.class);

    Network network = mock(Network.class);
    mockNetwork(network, proposedCon, validatedCon);

    // Verification.
    ProposerEngine proposerEngine = mockProposerEngine(local, accounts, block, network, pendingTransactions, state);
    newValidatedBlock(proposerEngine, block, pendingTransactions);

  }

  /**
   * Evaluates that when a new block is received but the block cannot be found on its storage.
   * It should throw an IllegalStateException.
   */
  @Test
  public void blockNotInDatabase() {
    Identifier localId = IdentifierFixture.newIdentifier();
    PrivateKey localPrivateKey = KeyGenFixture.newKeyGen().getPrivateKey();
    Local local = new Local(localId, localPrivateKey);
    ArrayList<Account> accounts = AccountFixture.newAccounts(11);
    Block block = BlockFixture.newBlock(Parameters.MIN_TRANSACTIONS_NUM + 1);

    // Initialize mocked components.
    Assigner assigner = mock(Assigner.class);
    Transactions pendingTransactions = mock(Transactions.class);
    State state = mock(State.class);
    Network network = mock(Network.class);

    Blocks blocks = mock(Blocks.class);
    when(blocks.atHeight(block.getHeight())).thenReturn(BlockFixture.newBlock()); // another block

    // Verification.
//    ProposerEngine proposerEngine = new ProposerEngine(blocks, pendingTransactions, state, local, network, assigner);
//    Assertions.assertThrows(IllegalArgumentException.class, () -> {
//      proposerEngine.onNewValidatedBlock(block.getHeight(), block.id());
//    });
  }

  /**
   * Evaluates that when enough block approvals are received,
   * a validated block is created and sent to the network (including itself).
   */
  @Test
  public void enoughBlockApproval() throws LightChainNetworkingException {
    Identifier localId = IdentifierFixture.newIdentifier();
    PrivateKey localPrivateKey = KeyGenFixture.newKeyGen().getPrivateKey();
    Local local = new Local(localId, localPrivateKey);
    ArrayList<Account> accounts = AccountFixture.newAccounts(11);
    Block block = BlockFixture.newBlock(Parameters.MIN_TRANSACTIONS_NUM + 1);
    State state = mock(State.class);
    Transactions pendingTransactions = mock(Transactions.class);
    ConcurrentMap<Identifier, String> idToAddressMap = new ConcurrentHashMap<>();
    Conduit validatedCon = mock(Conduit.class);
    Conduit proposedCon = mock(Conduit.class);
    P2pNetwork network = mock(P2pNetwork.class);
    blockApproval(idToAddressMap, validatedCon, proposedCon, network);

    // Verification.
    ProposerEngine proposerEngine = mockProposerEngine(local, accounts, block, network, pendingTransactions, state);
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
    Identifier localId = IdentifierFixture.newIdentifier();
    PrivateKey localPrivateKey = KeyGenFixture.newKeyGen().getPrivateKey();
    Local local = new Local(localId, localPrivateKey);
    ArrayList<Account> accounts = AccountFixture.newAccounts(11);
    Block block = BlockFixture.newBlock(Parameters.MIN_TRANSACTIONS_NUM + 1);
    // Initialize mocked components.

    State state = mock(State.class);
    Transactions pendingTransactions = mock(Transactions.class);
    ConcurrentMap<Identifier, String> idToAddressMap = new ConcurrentHashMap<>();
    Conduit validatedCon = mock(Conduit.class);
    Conduit proposedCon = mock(Conduit.class);
    P2pNetwork network = mock(P2pNetwork.class);
    blockApproval(idToAddressMap, validatedCon, proposedCon, network);

    // Verification.
    ProposerEngine proposerEngine = mockProposerEngine(local, accounts, block, network, pendingTransactions, state);
    proposerEngine.onNewValidatedBlock(block.getHeight(), block.id());
    blockApprovalConcurrently(proposerEngine, block);

    verify(validatedCon, times(1)).unicast(any(Block.class), any(Identifier.class));
  }

  /**
   * Evaluates that when enough block approvals are received concurrently,
   * a validated block is created and sent to the network (including itself).
   *
   * @param proposerEngine proposerEngine of node.
   * @param block          the block itself.
   * @throws InterruptedException unhappy path exception.
   */
  public void blockApprovalConcurrently(ProposerEngine proposerEngine, Block block) throws InterruptedException {
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
  }

  /**
   * Evaluates that when enough block approvals are received.
   *
   * @param idToAddressMap Concurrent map from id to address.
   * @param validatedCon   conduits for validated ones.
   * @param proposedCon    conduits for proposed ones.
   * @param network        network of nodes.
   * @throws LightChainNetworkingException unhappy path.
   */
  public void blockApproval(ConcurrentMap<Identifier, String> idToAddressMap, Conduit validatedCon, Conduit proposedCon,
                            P2pNetwork network) throws LightChainNetworkingException {
    idToAddressMap.put(IdentifierFixture.newIdentifier(), Channels.ValidatedBlocks);
    doNothing().when(validatedCon).unicast(any(Block.class), any(Identifier.class));
    doNothing().when(validatedCon).unicast(any(Block.class), any(Identifier.class));
    when(network.getIdToAddressMap()).thenReturn(idToAddressMap);
    when(network.register(any(Engine.class), eq(Channels.ProposedBlocks))).thenReturn(proposedCon);
    when(network.register(any(Engine.class), eq(Channels.ValidatedBlocks))).thenReturn(validatedCon);
  }

  /**
   * Mock the network.
   *
   * @param network      network of nodes.
   * @param proposedCon  conduits for proposed ones.
   * @param validatedCon conduits for validated ones.
   */
  public void mockNetwork(Network network, Conduit proposedCon, Conduit validatedCon) {
    when(network.register(any(Engine.class), eq(Channels.ProposedBlocks))).thenReturn(proposedCon);
    when(network.register(any(Engine.class), eq(Channels.ValidatedBlocks))).thenReturn(validatedCon);
  }

  /**
   * Sets up the mocks for proposer engine for processing.
   *
   * @param local               Local represents the set of utilities available to the current LightChain node.
   * @param accounts            array list of  LightChain account which is the constituent of the SnapShot.
   * @param block               the block itself.
   * @param network             network of nodes.
   * @param pendingTransactions Pending transactions storage.
   * @param state               State storage.
   * @return mocked proposerEngine.
   */
  private ProposerEngine mockProposerEngine(Local local, ArrayList<Account> accounts, Block block, Network network,
                                            Transactions pendingTransactions, State state) {
    Assignment assignment = mock(Assignment.class);
    Assigner assigner = mock(Assigner.class);
    Blocks blocks = mock(Blocks.class);
    Snapshot snapshot = mock(Snapshot.class);

    mockAssignment(assignment, assigner, pendingTransactions, local, blocks, block, accounts, snapshot, state);
    return null;
  }

  /**
   * Sets the assignment.
   */
  public void mockAssignment(Assignment assignment, Assigner assigner,
                             Transactions pendingTransactions, Local local, Blocks blocks, Block block,
                             ArrayList<Account> accounts, Snapshot snapshot, State state) {

    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all identifiers
    when(assignment.has(local.myId())).thenReturn(true);
    when(assignment.all()).thenReturn(IdentifierFixture.newIdentifiers(Parameters.VALIDATOR_THRESHOLD));
    when(assigner.assign(any(Identifier.class), any(Snapshot.class), any(short.class))).thenReturn(assignment);
    when(pendingTransactions.size()).thenReturn(Parameters.MIN_TRANSACTIONS_NUM + 1);
    when(pendingTransactions.all()).thenReturn(new ArrayList<>(Arrays.asList(block.getTransactions())));
    when(blocks.atHeight(block.getHeight())).thenReturn(block); // block to be proposed
    when(snapshot.all()).thenReturn(accounts);
    when(snapshot.getAccount(local.myId())).thenReturn(accounts.get(0));
    when(state.atBlockId(block.id())).thenReturn(snapshot);
  }

  /**
   * Evaluates that when a new block without enough validated transactions arrives at the proposer engine,
   * it keeps waiting till it finds enough transactions in its pending transaction database,
   * and then it creates a valid block and sends it to its assigners.
   */
  public void waitEnoughTransactions(ProposerEngine proposerEngine,
                                     Block block, Conduit proposedCon,
                                     Transactions pendingTransactions, State state)
      throws LightChainNetworkingException, InterruptedException {

    AtomicBoolean proposerWaiting = new AtomicBoolean(true);
    Thread proposerThread = new Thread(() -> {
      proposerEngine.onNewValidatedBlock(block.getHeight(), block.id());
      proposerWaiting.set(false);
    });
    proposerThread.start(); // start proposer thread
    verify(proposedCon, times(0)).unicast(any(Block.class), any(Identifier.class));
    Assertions.assertTrue(proposerWaiting.get()); // proposer should be waiting
    AtomicInteger transactionsCounter = new AtomicInteger(Parameters.MIN_TRANSACTIONS_NUM - 1);
    Thread ingestThread = new Thread(() -> {
      when(pendingTransactions.size()).thenReturn(transactionsCounter.incrementAndGet());
    });
    ingestThread.start(); // start ingest thread
    proposerThread.join(); // wait for proposer to finish
    ingestThread.join(); // wait for ingest to finish
    BlockValidator blockValidator = new BlockValidator(state);
    // Assertions.assertTrue(blockValidator.isCorrect(proposerEngine.newB));

  }

  /**
   * OnNewFinalizedBlock notifies the proposer engine of a new validated block. The proposer engine runs validator
   * assigner with the proposer tag and number of validators of 1. If this node is selected, it means that the
   * proposer engine must create a new block.
   */
  public void newValidatedBlock(ProposerEngine proposerEngine, Block block, Transactions pendingTransactions) {
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

}