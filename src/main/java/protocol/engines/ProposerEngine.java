package protocol.engines;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import model.Entity;
import model.codec.EntityType;
import model.crypto.Signature;
import model.exceptions.LightChainNetworkingException;
import model.lightchain.*;
import model.local.Local;
import network.Channels;
import network.Conduit;
import network.Network;
import network.p2p.P2pNetwork;
import protocol.Engine;
import protocol.NewBlockSubscriber;
import protocol.Parameters;
import protocol.Tags;
import protocol.assigner.LightChainValidatorAssigner;
import protocol.engines.common.LightchainAssignment;
import state.State;
import storage.Blocks;
import storage.Transactions;

/**
 * Proposer engine encapsulates the logic of creating new blocks.
 */
public class ProposerEngine implements NewBlockSubscriber, Engine {
  private final ReentrantLock lock = new ReentrantLock();
  private final Local local;
  private final Blocks blocks;
  private final Transactions pendingTransactions;
  private final State state;
  private final Conduit proposerCon;
  private final Conduit validatedCon;
  private final Network network;
  private final LightChainValidatorAssigner assigner;
  private final ArrayList<BlockApproval> approvals;
  private Block lastProposedBlock; // last proposed block that is pending validation.

  /**
   * Constructor.
   *
   * @param blocks              the blocks storage database.
   * @param pendingTransactions the pending transactions storage database.
   * @param state               the protocol state.
   * @param local               the local module of the node.
   * @param network             the network module of the node.
   * @param assigner            the validator assigner module.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "fields are intentionally mutable externally")
  public ProposerEngine(Blocks blocks,
                        Transactions pendingTransactions,
                        State state,
                        Local local,
                        Network network,
                        LightChainValidatorAssigner assigner) {
    this.local = local;
    this.blocks = blocks;
    this.pendingTransactions = pendingTransactions;
    this.state = state;
    this.approvals = new ArrayList<>();
    this.network = network;
    this.assigner = assigner;

    proposerCon = network.register(this, Channels.ProposedBlocks);
    validatedCon = network.register(this, Channels.ValidatedBlocks);
  }

  /**
   * OnNewFinalizedBlock notifies the proposer engine of a new validated block. The proposer engine runs validator
   * assigner with the proposer tag and number of validators of 1. If this node is selected, it means that the
   * proposer engine must create a new block.
   * ---
   * Creating new block: proposer engine has a shared storage component with ingest engine, i.e., transactions and
   * blocks. If the minimum number of validated transactions in the pending transactions' storage are available, then
   * proposer engine fetches them, creates a block out of them, runs validator assignment with validation tag, and
   * sends it to the validators. If it does not have minimum number of validated transactions, it waits till it
   * the minimum number is satisfied.
   *
   * @param blockHeight block height.
   * @param blockId     identifier of block.
   * @throws IllegalStateException    when it receives a new validated block while it is pending for its previously
   *                                  proposed block to get validated.
   * @throws IllegalArgumentException when its parameters do not match a validated block from database.
   */
  @Override
  public void onNewValidatedBlock(
      int blockHeight,
      Identifier blockId) throws IllegalStateException, IllegalArgumentException {

    if (!blocks.has(blockId) && !blocks.atHeight(blockHeight).id().equals(blockId)) {
      throw new IllegalArgumentException("block is not in database");
    }

    try {
      lock.lock();

      if (!this.nextBlockProposer(blockId)) {
        // this node is not proposer of the next block.
        return;
      }


      while (pendingTransactions.size() < Parameters.MIN_TRANSACTIONS_NUM) {
        // Waits until there are enough pending transactions.
        System.out.println("waiting for minimum validated transactions to propose next block");
        try {
          Thread.sleep(5000);
        } catch (InterruptedException ex) {
          throw new IllegalStateException("thread sleep interrupted", ex);
        }
      }

      // loads validated transactions to put in the next new block.
      ValidatedTransaction[] transactions = new ValidatedTransaction[Parameters.MIN_TRANSACTIONS_NUM];
      for (int i = 0; i < Parameters.MIN_TRANSACTIONS_NUM; i++) {
        Transaction tx = pendingTransactions.all().get(i);
        transactions[i] = ((ValidatedTransaction) tx);
        pendingTransactions.remove(tx.id());
      }

      Block nextProposedBlock = new Block(blockId, local.myId(), blockHeight + 1, transactions);
      Signature sign = local.signEntity(nextProposedBlock);
      nextProposedBlock.setSignature(sign);

      Assignment validators = LightchainAssignment.getValidators(
          nextProposedBlock.id(),
          assigner,
          this.state.atBlockId(blockId));

      // Adds the Block Proposer tag to the assigner.
      for (Identifier id : validators.all()) {
        try {
          this.proposerCon.unicast(nextProposedBlock, id);
        } catch (LightChainNetworkingException e) {
          throw new IllegalStateException("could not unicast new block for validation", e);
        }
      }

      lastProposedBlock = nextProposedBlock;
    } finally {
      lock.unlock();
    }
  }

  /**
   * The received entity must be only of the BlockApproval type.
   * When a BlockApproval arrives, proposer engine checks if the approval belongs for its recently proposed block, and
   * if it is the case, the approval is stored. When the proposer engine obtains enough approval on its recently
   * proposed block, it creates a validated block out of them and sends it to all nodes (including itself) over the
   * network using the validated blocks channel.
   *
   * @param e the arrived Entity from the network.
   * @throws IllegalArgumentException any entity other than BlockApproval.
   */
  @Override
  public void process(Entity e) throws IllegalArgumentException {
    if (Objects.equals(e.type(), EntityType.TYPE_BLOCK_APPROVAL) || ((BlockApproval) e).getBlockId() == null) {
      approvals.add((BlockApproval) e);
      if (approvals.size() >= Parameters.VALIDATOR_THRESHOLD) {
        Signature[] signs = new Signature[Parameters.VALIDATOR_THRESHOLD];
        for (int i = 0; i < approvals.size(); i++) {
          signs[i] = approvals.get(i).getSignature();
        }
        ValidatedBlock validatedBlock = new ValidatedBlock(
            newB.getPreviousBlockId(),
            newB.getProposer(),
            newB.getTransactions(),
            local.signEntity(newB),
            signs,
            newB.getHeight());
        for (Map.Entry<Identifier, String> pair : ((P2pNetwork) network).getIdToAddressMap().entrySet()) {
          if (pair.getValue().equals(Channels.ValidatedBlocks)) {
            try {
              validatedCon.unicast(validatedBlock, pair.getKey());
            } catch (LightChainNetworkingException e1) {
              e1.printStackTrace();
            }
          }
        }
        approvals.clear();
        newB = null;
      }

    } else {
      throw new IllegalArgumentException("entity is not of type BlockApproval");
    }
  }

  /**
   * Given a block id, checks whether this node is the proposer of the next block.
   *
   * @param blockId current block id.
   * @return true if this node is the proposer of the next block, false otherwise.
   * @throws IllegalStateException on the unhappy path of performing proposer assignment.
   */
  private boolean nextBlockProposer(Identifier blockId) throws IllegalStateException {
    // Adds the Block Proposer tag to the assigner.
    byte[] bytesId = blockId.getBytes();
    byte[] bytesTag = Tags.BlockProposerTag.getBytes(StandardCharsets.UTF_8);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try {
      output.write(bytesId, 0, bytesId.length);
      output.write(bytesTag);
    } catch (IOException e) {
      throw new IllegalStateException("could not write to bytes to ByteArrayOutputStream", e);
    }
    Identifier taggedId = new Identifier(output.toByteArray());

    // TODO: needs test cases to ensure correctness of choosing exactly one (at validator side).
    Assignment assignment = assigner.assign(taggedId, state.atBlockId(blockId), (short) 1);
    return assignment.has(local.myId());
  }
}