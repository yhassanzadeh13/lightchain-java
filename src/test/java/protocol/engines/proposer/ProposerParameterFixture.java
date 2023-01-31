package protocol.engines.proposer;

import java.util.ArrayList;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import model.lightchain.Account;
import model.lightchain.Assignment;
import model.lightchain.Block;
import model.lightchain.BlockProposal;
import model.lightchain.Identifier;
import model.lightchain.ValidatedTransaction;
import network.Channels;
import network.Conduit;
import network.Network;
import org.apache.commons.compress.utils.Lists;
import protocol.Engine;
import protocol.Parameters;
import protocol.assigner.ProposerAssigner;
import protocol.assigner.ValidatorAssigner;
import state.Snapshot;
import state.State;
import storage.BlockProposals;
import storage.Blocks;
import storage.Transactions;
import unittest.fixtures.IdentifierFixture;
import unittest.fixtures.LocalFixture;
import unittest.fixtures.ValidatedTransactionFixture;

/**
 * Encapsulates mocked version of the proposer parameters for testing.
 */
public class ProposerParameterFixture extends ProposerParameters {
  public Conduit proposedConduit;
  public Conduit validatedConduit;

  /**
   * Default constructor of this fixture, sets everything to mock.
   */
  public ProposerParameterFixture() {
    super();

    this.blocks = mock(Blocks.class);
    this.pendingTransactions = mock(Transactions.class);
    this.state = mock(State.class);
    this.local = LocalFixture.newLocal();
    this.network = mock(Network.class);
    this.validatorAssigner = mock(ValidatorAssigner.class);
    this.proposerAssigner = mock(ProposerAssigner.class);
    this.blockProposals = mock(BlockProposals.class);

    this.proposedConduit = mock(Conduit.class);
    this.validatedConduit = mock(Conduit.class);
    when(network.register(any(Engine.class), eq(Channels.ProposedBlocks))).thenReturn(this.proposedConduit);
    when(network.register(any(Engine.class), eq(Channels.ValidatedBlocks))).thenReturn(this.validatedConduit);
  }

  /**
   * Mocks existence of the given block in the blocks storage of this fixture.
   *
   * @param block block to be mocked for existence.
   */
  public void mockBlocksStorageForBlock(Block block) {
    when(blocks.byId(block.id())).thenReturn(block);
  }

  /**
   * Mocks the current node of this fixture as the proposer of the next block, given the current block.
   *
   * @param currentBlock the current block for which the next block is proposed.
   */
  public void mockIdAsNextBlockProposer(Block currentBlock) {
    Snapshot snapshot = mock(Snapshot.class);
    when(this.state.atBlockId(currentBlock.id())).thenReturn(snapshot);

    // mocking account of this node at snapshot for later verifying its signature on the block.
    when(snapshot.getAccount(this.local.myId())).thenReturn(
        new Account(this.local.myId(), this.local.myPublicKey(), IdentifierFixture.newIdentifier(), Parameters.MINIMUM_STAKE));
    when(this.proposerAssigner.nextBlockProposer(currentBlock.id(), snapshot)).thenReturn(this.local.myId());
  }

  /**
   * Mocks a different random node (than the current node of this fixture) as the proposer of the next block, given the current block.
   *
   * @param currentBlock the current block for which the next block is proposed.
   */
  public void mockDifferentNodeAsNextBlockProposer(Block currentBlock) {
    Snapshot snapshot = mock(Snapshot.class);
    when(this.state.atBlockId(currentBlock.id())).thenReturn(snapshot);

    // mocking account of this node at snapshot for later verifying its signature on the block.
    when(snapshot.getAccount(this.local.myId())).thenReturn(
        new Account(this.local.myId(), this.local.myPublicKey(), IdentifierFixture.newIdentifier(), Parameters.MINIMUM_STAKE));
    when(this.proposerAssigner.nextBlockProposer(currentBlock.id(), snapshot)).thenReturn(IdentifierFixture.newIdentifier());
  }

  /**
   * Mocks given identifiers as the result of any validator assignment on the validator assigner of this fixture.
   *
   * @param validators list of validators identifiers.
   */
  public void mockValidatorAssigner(ArrayList<Identifier> validators) {
    Assignment assignment = new Assignment();
    for (Identifier validator : validators) {
      assignment.add(validator);
    }
    when(this.validatorAssigner.getValidatorsAtSnapshot(any(Identifier.class), any(Snapshot.class))).thenReturn(assignment);
  }

  /**
   * Mocks BlockProposal storage of this fixture with given BlockProposal as the "last proposed block".
   *
   * @param proposal given BlockProposal to be mocked as the last proposed block.
   */
  public void mockBlockProposal(BlockProposal proposal) {
    when(this.blockProposals.getLastProposal()).thenReturn(proposal);
  }

  /**
   * Mocks state of this fixture with the given accounts at the given block id.
   *
   * @param accounts accounts to mock the state with.
   * @param blockId  block id corresponding to the state snapshot for these accounts.
   */
  public void mockSnapshotAtBlock(ArrayList<Account> accounts, Identifier blockId) {
    Snapshot snapshot = mock(Snapshot.class);
    when(this.state.atBlockId(blockId)).thenReturn(snapshot);
    when(snapshot.all()).thenReturn(accounts);
  }

  /**
   * Generates validated transaction fixtures and mock a Transactions storage with it.
   *
   * @param count total validated transactions to be created.
   */
  public void mockValidatedTransactions(int count) {
    ValidatedTransaction[] transactions = ValidatedTransactionFixture.newValidatedTransactions(count);
    when(this.pendingTransactions.size()).thenReturn(transactions.length);
    when(this.pendingTransactions.all()).thenReturn(Lists.newArrayList(Arrays.stream(transactions).iterator()));
  }

}
