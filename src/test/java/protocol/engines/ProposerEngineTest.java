package protocol.engines;

import model.crypto.PrivateKey;
import model.lightchain.Account;
import model.lightchain.Block;
import model.lightchain.Identifier;
import model.local.Local;
import network.Channels;
import network.Network;
import network.NetworkAdapter;
import networking.MockConduit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import protocol.block.BlockValidator;
import state.Snapshot;
import state.State;
import storage.Blocks;
import storage.Transactions;
import unittest.fixtures.AccountFixture;
import unittest.fixtures.BlockFixture;
import unittest.fixtures.IdentifierFixture;
import unittest.fixtures.KeyGenFixture;

import java.util.ArrayList;

import static org.mockito.Mockito.mock;

/**
 * Encapsulates tests for the proposer engine.
 */
public class ProposerEngineTest {
  private ProposerEngine proposerEngine;
  private Snapshot snapshot;
  private Block block1;
  private State state;
  private Network network;
  private Local local;
  private Blocks blocks;
  private Transactions pendingTransactions;

  private MockConduit proposedCon;
  private MockConduit validatedCon;
  private Identifier localId;
  private PrivateKey localPrivateKey;
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
  @BeforeEach
  public void setUpMock() {

    localId = IdentifierFixture.newIdentifier();
    localPrivateKey = KeyGenFixture.newKeyGen().getPrivateKey();
    local = new Local(localId, localPrivateKey);
    pendingTransactions = mock(Transactions.class);
    blocks = mock(Blocks.class);
    block1 = BlockFixture.newBlock();
    state = mock(State.class);
    snapshot = mock(Snapshot.class);
    network = mock(Network.class);
    NetworkAdapter networkAdapter = mock(NetworkAdapter.class);
    proposedCon = new MockConduit(Channels.ProposedBlocks, networkAdapter);
    validatedCon = new MockConduit(Channels.ValidatedBlocks, networkAdapter);
    ArrayList<Account>[] a = AccountFixture.newAccounts(11,0).values();

    state = mock(State.class);
    proposerEngine = new ProposerEngine(blocks,pendingTransactions,state,local,network);

  }
  @Test
  public void blockValidationTest(){

    BlockValidator blockValidator = new BlockValidator(state);


  }













}
