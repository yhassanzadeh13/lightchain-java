package protocol.engines;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.prometheus.client.Counter;
import metrics.collectors.LightChainCollector;
import metrics.collectors.MetricServer;
import metrics.integration.MetricsTestNet;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import protocol.Engine;
import protocol.Parameters;
import protocol.assigner.LightChainValidatorAssigner;
import protocol.block.BlockValidator;
import state.Snapshot;
import state.State;
import storage.Blocks;
import storage.Transactions;
import storage.mapdb.BlocksMapDb;
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

  static LightChainCollector collector;
  static Counter validatorElections;
  static Counter proposedBlocks;
  static Counter proposedValidBlocks;
  @BeforeAll
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
      validatorElections = collector.counter().register("additions_to_pending_count",
              "consensus", "ProposerEngine", "Counter for keeping the " +
                      "number of times this node is elected as a validator");
      proposedBlocks = collector.counter().register("proposed_block_count",
              "consensus", "ProposerEngine", "Counter for keeping the number of " +
                      "proposed blocks");
      proposedValidBlocks = collector.counter().register("proposed_valid_block_count",
              "consensus", "ProposerEngine", "Counter for keeping the number of " +
                      "proposed blocks that passed validation");

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

  @AfterAll
  public static void tearDown() {

    while(true);

  }

  /**
   * Evaluates that when enough block approvals are received,
   * a validated block is created and sent to the network (including itself).
   */
  @Test
  public void enoughBlockApproval() throws LightChainNetworkingException {

    while (true) {

      try {
        Thread.sleep(1000);
      } catch (InterruptedException ex) {
        System.err.println("Thread sleep issue, breaking the loop");
        break;
      }

      // Initialize non-mocked components.
      Identifier localId = IdentifierFixture.newIdentifier();
      PrivateKey localPrivateKey = KeyGenFixture.newKeyGen().getPrivateKey();
      Local local = new Local(localId, localPrivateKey);
      ArrayList<Account> accounts = AccountFixture.newAccounts(11);
      Block block = BlockFixture.newBlock(Parameters.MIN_TRANSACTIONS_NUM + 1);

      // Initialize mocked components.
      Assignment assignment = mock(Assignment.class);
      when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all identifiers
      when(assignment.has(local.myId())).thenReturn(true);

      LightChainValidatorAssigner assigner = mock(LightChainValidatorAssigner.class);
      when(assigner.assign(any(Identifier.class), any(Snapshot.class), any(short.class))).thenReturn(assignment);

      Transactions pendingTransactions = mock(Transactions.class);
      when(pendingTransactions.size()).thenReturn(Parameters.MIN_TRANSACTIONS_NUM + 1);
      when(pendingTransactions.all()).thenReturn(new ArrayList<>(Arrays.asList(block.getTransactions())));

      Path currentRelativePath = Paths.get("");
      Path tempdir = null;
      try {
        tempdir = Files.createTempDirectory(currentRelativePath, "tempdir");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      BlocksMapDb db = new BlocksMapDb(tempdir.toAbsolutePath() + "/" + "tempfileID.db",
              tempdir.toAbsolutePath() + "/" + "tempfileHEIGHT.db");
      Blocks blocks = db;

      db.add(block);

      // when(blocks.atHeight(block.getHeight())).thenReturn(block); // block to be proposed

      Snapshot snapshot = mock(Snapshot.class);
      when(snapshot.all()).thenReturn(accounts);
      when(snapshot.getAccount(localId)).thenReturn(accounts.get(0));

      State state = mock(State.class);
      when(state.atBlockId(block.id())).thenReturn(snapshot);

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
      ProposerEngine proposerEngine = new ProposerEngine(blocks, pendingTransactions, state, local, network, assigner,
              validatorElections, proposedBlocks, proposedValidBlocks);
      proposerEngine.onNewValidatedBlock(block.getHeight(), block.id());
      BlockValidator blockValidator = new BlockValidator(state);
      for (int i = 0; i < Parameters.VALIDATOR_THRESHOLD; i++) {
        BlockApproval blockApproval = new BlockApproval(SignatureFixture.newSignatureFixture(), block.id());
        proposerEngine.process(blockApproval);
      }
      verify(validatedCon, times(1)).unicast(any(Block.class), any(Identifier.class));


      db.closeDb();
      try {
        FileUtils.deleteDirectory(new File(tempdir.toString()));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

    }
  }


}
