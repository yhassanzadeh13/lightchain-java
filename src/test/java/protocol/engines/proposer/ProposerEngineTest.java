package protocol.engines.proposer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import model.Entity;
import model.crypto.KeyGen;
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
import protocol.Engine;
import protocol.Parameters;
import protocol.assigner.ProposerAssignerInf;
import protocol.assigner.ValidatorAssignerInf;
import protocol.assigner.lightchain.Assigner;
import protocol.block.BlockValidator;
import protocol.engines.proposer.ProposerEngine;
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
   * Evaluates happy path of ProposerEngine, i.e., proposing a new block to the validators when it is assigned as the proposer.
   */
  @Test
  public void happyPath() {
    Block currentBlock = BlockFixture.newBlock(Parameters.MIN_TRANSACTIONS_NUM + 1);

    ProposerParameterFixture params = new ProposerParameterFixture();
    params.mockBlocksStorageForBlock(currentBlock);
    // mocks this node as the proposer of the next block.
    params.mockIdAsNextBlockProposer(currentBlock);
    // mocks enough validated pending transactions.
    params.mockValidatedTransactions(1);
    // mocks validators
    ArrayList<Identifier> validators = IdentifierFixture.newIdentifiers(Parameters.VALIDATOR_THRESHOLD);
    params.mockValidatorAssigner(validators);

    // mocks networking layer and conduit registration.
    CountDownLatch blockSentForValidation = new CountDownLatch(Parameters.VALIDATOR_THRESHOLD);
    try {
      doAnswer(invocationOnMock -> {
        Block block = invocationOnMock.getArgument(0, Block.class);
        // checks whether block is correct and authenticated.
        BlockValidator validator = new BlockValidator(params.state);
        Assertions.assertTrue(validator.isCorrect(block));
        Assertions.assertTrue(validator.isAuthenticated(block));

        // block should be sent to an assigned validator for validation.
        Identifier validatorId = invocationOnMock.getArgument(1, Identifier.class);
        Assertions.assertTrue(validators.contains(validatorId));

        blockSentForValidation.countDown();
        return null;
      }).when(params.proposedConduit).unicast(any(Block.class), any(Identifier.class));
    } catch (LightChainNetworkingException e) {
      Assertions.fail();
    }


    ProposerEngine proposerEngine = new ProposerEngine(params);
    proposerEngine.onNewValidatedBlock(currentBlock.getHeight(), currentBlock.id());

    try {
      boolean doneOnTime = blockSentForValidation.await(1000, TimeUnit.MILLISECONDS);
      Assertions.assertTrue(doneOnTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
  }



  /**
   * Evaluates that when a new block is received while it is pending
   * for its previously proposed block to get validated, the engine throws an IllegalStateException.
   */
  @Test
  public void newValidatedBlockWhilePendingValidation() {
    Block block = BlockFixture.newBlock(Parameters.MIN_TRANSACTIONS_NUM + 1);

    ProposerParameterFixture params = new ProposerParameterFixture();
    params.mockBlocksStorageForBlock(block);
    // mocks an existing proposed block.
    when(params.blocks.byTag(Blocks.TAG_LAST_PROPOSED_BLOCK)).thenReturn(BlockFixture.newBlock());

    ProposerEngine engine = new ProposerEngine(params);
    Assertions.assertThrows(IllegalStateException.class, () -> {
      engine.onNewValidatedBlock(block.getHeight(), block.id());
    });
  }

  /**
   * Evaluates that when a new block is received but the block cannot be found on its storage.
   * It should throw an IllegalArgumentException.
   */
  @Test
  public void blockNotInDatabase() {
    Block block = BlockFixture.newBlock(Parameters.MIN_TRANSACTIONS_NUM + 1);

    ProposerParameterFixture params = new ProposerParameterFixture();
    // mocks an existing proposed block.
    when(params.blocks.has(block.id())).thenReturn(false);

    ProposerEngine proposerEngine = new ProposerEngine(params);
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      proposerEngine.onNewValidatedBlock(block.getHeight(), block.id());
    });
  }

  /**
   * Evaluates when unicasting next proposed block to any of the validators fails. The proposed block should never male persistent.
   */
  @Test
  public void failedUnicast() throws LightChainNetworkingException {
    Block currentBlock = BlockFixture.newBlock(Parameters.MIN_TRANSACTIONS_NUM + 1);

    ProposerParameterFixture params = new ProposerParameterFixture();
    params.mockBlocksStorageForBlock(currentBlock);
    // mocks this node as the proposer of the next block.
    params.mockIdAsNextBlockProposer(currentBlock);
    // mocks enough validated pending transactions.
    params.mockValidatedTransactions(1);
    // mocks validators
    ArrayList<Identifier> validators = IdentifierFixture.newIdentifiers(Parameters.VALIDATOR_THRESHOLD);
    params.mockValidatorAssigner(validators);

    // mocks unicasting faces an exception
    doThrow(new LightChainNetworkingException("exception", new Throwable())).when(params.proposedConduit)
        .unicast(any(Entity.class), any(Identifier.class));



    ProposerEngine proposerEngine = new ProposerEngine(params);

    Assertions.assertThrows(IllegalStateException.class, () -> {
      proposerEngine.onNewValidatedBlock(currentBlock.getHeight(), currentBlock.id());
    });

    // proposed block should never make persistent
    verify(params.blocks, never()).writeTag(eq(Blocks.TAG_LAST_PROPOSED_BLOCK), any(Block.class));
  }

  /**
   * Evaluates that when enough block approvals are received,
   * a validated block is created and sent to the network.
   */
  @Test
  public void enoughBlockApproval() throws LightChainNetworkingException {


    ProposerParameterFixture params = new ProposerParameterFixture();
    Block proposedBlock = new Block(
        IdentifierFixture.newIdentifier(),
        params.local.myId(),
        100,
        ValidatedTransactionFixture.newValidatedTransactions(10));
    params.mockProposedBlock(proposedBlock);
    ArrayList<Account> accounts = AccountFixture.newAccounts(10);
    params.mockSnapshotAtBlock(accounts, proposedBlock.getPreviousBlockId());


    // Verification.
    ProposerEngine proposerEngine = new ProposerEngine(params);

    for (int i = 0; i < Parameters.VALIDATOR_THRESHOLD; i++) {
      BlockApproval blockApproval = new BlockApproval(SignatureFixture.newSignatureFixture(), proposedBlock.id());
      proposerEngine.process(blockApproval);
    }

    try {
      doAnswer(invocationOnMock -> {
        ValidatedBlock block = invocationOnMock.getArgument(0, ValidatedBlock.class);

        // checks whether block is correct and authenticated.
        Assertions.assertTrue(params.local.myPublicKey().verifySignature(proposedBlock, block.getSignature()));
        // checks all fields of validated block matches with proposed block
        // TODO: also check for certificaties.
        Assertions.assertEquals(proposedBlock.getPreviousBlockId(), block.getPreviousBlockId());
        Assertions.assertEquals(proposedBlock.getProposer(), params.local.myId());
        Assertions.assertEquals(proposedBlock.getTransactions(), block.getTransactions());
        Assertions.assertEquals(proposedBlock.getHeight(), block.getHeight());

        return null;
      }).when(params.validatedConduit).unicast(any(ValidatedBlock.class), any(Identifier.class));
    } catch (LightChainNetworkingException e) {
      Assertions.fail();
    }

    // validated block must be sent to all nodes.
    verify(params.validatedConduit, times(10)).unicast(any(ValidatedBlock.class), any(Identifier.class));

  }

  /**
   * Evaluates that when enough block approvals are received concurrently,
   * a validated block is created and sent to the network (including itself).
   */
  @Test
  public void enoughBlockApprovalConcurrently() throws LightChainNetworkingException, InterruptedException {
    Block currentBlock = BlockFixture.newBlock(Parameters.MIN_TRANSACTIONS_NUM + 1);

    ProposerParameterFixture params = new ProposerParameterFixture();
    params.mockBlocksStorageForBlock(currentBlock);
    // mocks this node as the proposer of the next block.
    params.mockIdAsNextBlockProposer(currentBlock);
    // mocks enough validated pending transactions.
    params.mockValidatedTransactions(1);
    // mocks validators
    ArrayList<Identifier> validators = IdentifierFixture.newIdentifiers(Parameters.VALIDATOR_THRESHOLD);
    params.mockValidatorAssigner(validators);

    Identifier localId = IdentifierFixture.newIdentifier();
    PrivateKey localPrivateKey = KeyGenFixture.newKeyGen().getPrivateKey();
    KeyGen keyGen = KeyGenFixture.newKeyGen();
    Local local = new Local(localId, keyGen.getPrivateKey(), keyGen.getPublicKey());
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