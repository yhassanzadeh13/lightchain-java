package protocol.engines.proposer;

import java.util.ArrayList;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import model.lightchain.*;
import network.Channels;
import network.Conduit;
import network.Network;
import org.apache.commons.compress.utils.Lists;
import protocol.Engine;
import protocol.Parameters;
import protocol.assigner.ProposerAssignerInf;
import protocol.assigner.ValidatorAssignerInf;
import state.Snapshot;
import state.State;
import storage.Blocks;
import storage.Transactions;
import unittest.fixtures.IdentifierFixture;
import unittest.fixtures.LocalFixture;
import unittest.fixtures.ValidatedTransactionFixture;

public class ProposerParameterFixture extends ProposerParameters {
  public Conduit proposedConduit;
  public Conduit validatedConduit;

  public ProposerParameterFixture() {
    super();

    this.blocks = mock(Blocks.class);
    this.pendingTransactions = mock(Transactions.class);
    this.state = mock(State.class);
    this.local = LocalFixture.newLocal();
    this.network = mock(Network.class);
    this.validatorAssigner = mock(ValidatorAssignerInf.class);
    this.proposerAssigner = mock(ProposerAssignerInf.class);

    this.proposedConduit = mock(Conduit.class);
    this.validatedConduit = mock(Conduit.class);
    when(network.register(any(Engine.class), eq(Channels.ProposedBlocks))).thenReturn(this.proposedConduit);
    when(network.register(any(Engine.class), eq(Channels.ValidatedBlocks))).thenReturn(this.validatedConduit);
  }


  public void mockBlocksStorageForBlock(Block block) {
    when(blocks.byId(block.id())).thenReturn(block);
  }

  public void mockIdAsNextBlockProposer(Block currentBlock) {
    Snapshot snapshot = mock(Snapshot.class);
    when(this.state.atBlockId(currentBlock.id())).thenReturn(snapshot);

    // mocking account of this node at snapshot for later verifying its signature on the block.
    when(snapshot.getAccount(this.local.myId())).thenReturn(
        new Account(this.local.myId(), this.local.myPublicKey(), IdentifierFixture.newIdentifier(), Parameters.MINIMUM_STAKE));
    when(this.proposerAssigner.nextBlockProposer(currentBlock.id(), snapshot)).thenReturn(this.local.myId());
  }

  public void mockDifferentNodeAsNextBlockProposer(Block currentBlock) {
    Snapshot snapshot = mock(Snapshot.class);
    when(this.state.atBlockId(currentBlock.id())).thenReturn(snapshot);

    // mocking account of this node at snapshot for later verifying its signature on the block.
    when(snapshot.getAccount(this.local.myId())).thenReturn(
        new Account(this.local.myId(), this.local.myPublicKey(), IdentifierFixture.newIdentifier(), Parameters.MINIMUM_STAKE));
    when(this.proposerAssigner.nextBlockProposer(currentBlock.id(), snapshot)).thenReturn(IdentifierFixture.newIdentifier());
  }

  public void mockValidatorAssigner(ArrayList<Identifier> validators) {
    Assignment assignment = new Assignment();
    for (Identifier validator : validators) {
      assignment.add(validator);
    }
    when(this.validatorAssigner.getValidatorsAtSnapshot(any(Identifier.class), any(Snapshot.class))).thenReturn(assignment);
  }

  public void mockProposedBlock(Block block) {
    when(this.blocks.byTag(Blocks.TAG_LAST_PROPOSED_BLOCK)).thenReturn(block);
  }

  public void mockSnapshotAtBlock(ArrayList<Account> accounts, Identifier blockId) {
    Snapshot snapshot = mock(Snapshot.class);
    when(this.state.atBlockId(blockId)).thenReturn(snapshot);
    when(snapshot.all()).thenReturn(accounts);
  }

  /**
   * Generates validated transaction fixtures and mock a Transactions storage with it.
   *
   * @param count total validated transactions to be created.
   * @return a Transactions storage which mocked with validated transactions.
   */
  public void mockValidatedTransactions(int count) {
    ValidatedTransaction[] transactions = ValidatedTransactionFixture.newValidatedTransactions(count);
    when(this.pendingTransactions.size()).thenReturn(transactions.length);
    when(this.pendingTransactions.all()).thenReturn(Lists.newArrayList(Arrays.stream(transactions).iterator()));
  }

}
