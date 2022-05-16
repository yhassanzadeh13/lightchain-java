package protocol.engines;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Random;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import model.crypto.PrivateKey;
import model.lightchain.Account;
import model.lightchain.Assignment;
import model.lightchain.Block;
import model.lightchain.Identifier;
import model.local.Local;
import network.Channels;
import network.Network;
import network.NetworkAdapter;
import networking.MockConduit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import protocol.Engine;
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
    Random random = new Random();
    Identifier localId = IdentifierFixture.newIdentifier();
    PrivateKey localPrivateKey = KeyGenFixture.newKeyGen().getPrivateKey();
    Local local = new Local(localId, localPrivateKey);
    ArrayList<Account> accounts = AccountFixture.newAccounts(11);
    Block block = ValidatedBlockFixture.newValidatedBlock(accounts,Math.abs(random.nextInt(1_000_000)),IdentifierFixture.newIdentifier());

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
    when(snapshot.all()).thenReturn(accounts);
    when(snapshot.getAccount(localId)).thenReturn(accounts.get(0));
    when(assignment.has(local.myId())).thenReturn(true);
    when(state.atBlockId(block.id())).thenReturn(snapshot);
    when(pendingTransactions.size()).thenReturn(1);
    when(pendingTransactions.all()).thenReturn(new ArrayList<>(Arrays.asList(block.getTransactions())));
    when(network.register(any(Engine.class), eq(Channels.ProposedBlocks))).thenReturn(proposedCon);
    when(network.register(any(Engine.class), eq(Channels.ValidatedBlocks))).thenReturn(validatedCon);

    // Verification.
    ProposerEngine proposerEngine = new ProposerEngine(blocks, pendingTransactions, state, local, network, assignment);
    proposerEngine.onNewValidatedBlock(block.getHeight(), block.id());
    BlockValidator blockValidator = new BlockValidator(state);
    Assertions.assertTrue(blockValidator.isCorrect(proposerEngine.newB));
  }
}
