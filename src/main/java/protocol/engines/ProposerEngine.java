package protocol.engines;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import model.Convert;
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
import org.apache.log4j.Logger;
import protocol.Engine;
import protocol.NewBlockSubscriber;
import protocol.Parameters;
import protocol.assigner.ProposerAssignerInf;
import protocol.assigner.ValidatorAssignerInf;
import state.State;
import storage.Blocks;
import storage.Transactions;

/**
 * Proposer engine encapsulates the logic of creating new blocks.
 */
public class ProposerEngine implements NewBlockSubscriber, Engine {
  private final Logger logger;
  private final ReentrantLock lock = new ReentrantLock();
  private final Local local;
  private final Blocks blocks;
  private final Transactions pendingTransactions;
  private final State state;
  private final Conduit proposerCon;
  private final Conduit validatedCon;
  private final Network network;
  private final ProposerAssignerInf proposerAssigner;
  private final ValidatorAssignerInf validatorAssigner;
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
                        ValidatorAssignerInf validatorAssigner,
                        ProposerAssignerInf proposerAssigner) {
    this.local = local;
    this.blocks = blocks;
    this.pendingTransactions = pendingTransactions;
    this.state = state;
    this.approvals = new ArrayList<>();
    this.network = network;
    this.proposerAssigner = proposerAssigner;
    this.validatorAssigner = validatorAssigner;
    this.logger = Logger.getLogger(ProposerEngine.class.getName());

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

    this.logger.debug("new validated block arrived, block_id: " + blockId.toString());

    if (!blocks.has(blockId)) {
      this.logger.error("validated block is not in database");
      throw new IllegalArgumentException("block is not in database");
    }

    Identifier retrievedBlockId = blocks.atHeight(blockHeight).id();
    if (!retrievedBlockId.equals(blockId)) {
      this.logger.error("block mismatch on data base retrieval, expected: " + blockId + " got: " + retrievedBlockId);
      throw new IllegalStateException("block mismatch on data base retrieval, expected: " + blockId + " got: " + retrievedBlockId);
    }

    try {
      lock.lock();

      if (this.proposerAssigner.nextBlockProposer(blockId, state.atBlockId(blockId)).equals(this.local.myId())) {
        // this node is not proposer of the next block.
        this.logger.debug("this node is not the proposer of next block, current_block_id: " + blockId);
        return;
      }

      while (checkPendingTransactionsWithBackoff(Parameters.MIN_TRANSACTIONS_NUM, 5000)) {
        this.logger.debug("busy waiting for enough pending transactions");
      }

      ValidatedTransaction[] transactions = this.collectTransactionsForBlock(Parameters.MIN_TRANSACTIONS_NUM);
      this.logger.debug("pending validated transactions collected for the next block, total: " + transactions.length
          + " ids: " + Convert.IdentifierOf(transactions));

      Block nextProposedBlock = new Block(blockId, local.myId(), blockHeight + 1, transactions);
      Signature sign = local.signEntity(nextProposedBlock);
      nextProposedBlock.setSignature(sign);


      Assignment validators = this.validatorAssigner.getValidatorsAtSnapshot(nextProposedBlock.id(), this.state.atBlockId(blockId));

      // Sends block to validators.
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
    if (!e.type().equals(EntityType.TYPE_BLOCK_APPROVAL)) {
      throw new IllegalArgumentException("unexpected input at process engine: " + e.type());
    }
    if (Objects.equals(e.type(), EntityType.TYPE_BLOCK_APPROVAL) || ((BlockApproval) e).getBlockId() == null) {
      approvals.add((BlockApproval) e);
      if (approvals.size() >= Parameters.VALIDATOR_THRESHOLD) {
        Signature[] signs = new Signature[Parameters.VALIDATOR_THRESHOLD];
        for (int i = 0; i < approvals.size(); i++) {
          signs[i] = approvals.get(i).getSignature();
        }
        ValidatedBlock validatedBlock = new ValidatedBlock(
            lastProposedBlock.getPreviousBlockId(),
            lastProposedBlock.getProposer(),
            lastProposedBlock.getTransactions(),
            local.signEntity(lastProposedBlock),
            signs,
            lastProposedBlock.getHeight());
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
        lastProposedBlock = null;
      }

    } else {
      throw new IllegalArgumentException("entity is not of type BlockApproval");
    }
  }

  private ValidatedTransaction[] collectTransactionsForBlock(int count) {
    ValidatedTransaction[] transactions = new ValidatedTransaction[count];

    // loads validated transactions to put in the next new block.
    ArrayList<Transaction> pendingTx = pendingTransactions.all();

    for (int i = 0; i < count; i++) {
      // TODO: pending transactions must return a ValidatedTransaction.
      Transaction tx = pendingTx.get(i);
      transactions[i] = (ValidatedTransaction) tx;
      pendingTransactions.remove(tx.id());
    }

    return transactions;
  }

  /**
   * Checks pending transaction storage to see if it has at least count-many transactions. It waits "millis" milliseconds
   * prior to returning a false, to account for a retry backoff.
   *
   * @param count  number of desired pending transactions to exist.
   * @param millis delay in milliseconds before returning false.
   * @return true if at least count-many transactions exist in pending transactions storage, and false otherwise. A true is returned immediately,
   * while a false is returned with a "millis" milliseconds delay.
   */
  private boolean checkPendingTransactionsWithBackoff(int count, long millis) throws IllegalStateException {
    // TODO: put this in a new function
    if (pendingTransactions.size() < Parameters.MIN_TRANSACTIONS_NUM) {
      // TODO: optimize busy wating
      // Waits until there are enough pending transactions.
      this.logger.warn("waiting for enough (" + Parameters.MIN_TRANSACTIONS_NUM
          + ") pending transactions, pending_transactions_size: " + pendingTransactions.size());
      try {
        Thread.sleep(millis);
      } catch (InterruptedException ex) {
        throw new IllegalStateException("thread sleep interrupted", ex);
      }

      return false;
    }
    return true;
  }
}