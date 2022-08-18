package protocol.engines.proposer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import model.Convert;
import model.Entity;
import model.codec.EntityType;
import model.crypto.Signature;
import model.exceptions.LightChainNetworkingException;
import model.lightchain.*;
import model.local.Local;
import modules.logger.LightchainLogger;
import network.Channels;
import network.Conduit;
import org.slf4j.Logger;
import protocol.Engine;
import protocol.NewBlockSubscriber;
import protocol.Parameters;
import protocol.assigner.ProposerAssigner;
import protocol.assigner.ValidatorAssigner;
import state.Snapshot;
import state.State;
import storage.BlockProposals;
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
  private final ProposerAssigner proposerAssigner;
  private final ValidatorAssigner validatorAssigner;
  private final ArrayList<BlockApproval> approvals;
  private final BlockProposals blockProposals;

  /**
   * Constructor.
   *
   * @param parameters the parameters object for building proposer engine.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "fields are intentionally mutable externally")
  public ProposerEngine(ProposerParameters parameters) {
    this.local = parameters.local;
    this.blocks = parameters.blocks;
    this.pendingTransactions = parameters.pendingTransactions;
    this.state = parameters.state;
    this.proposerAssigner = parameters.proposerAssigner;
    this.validatorAssigner = parameters.validatorAssigner;
    this.blockProposals = parameters.blockProposals;

    this.approvals = new ArrayList<>();
    this.logger = LightchainLogger.getLogger(ProposerEngine.class.getCanonicalName());

    proposerCon = parameters.network.register(this, Channels.ProposedBlocks);
    validatedCon = parameters.network.register(this, Channels.ValidatedBlocks);
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
   * @param blockId identifier of block.
   * @throws IllegalStateException    when it receives a new validated block while it is pending for its previously
   *                                  proposed block to get validated.
   * @throws IllegalArgumentException when its parameters do not match a validated block from database.
   */
  @Override
  public void onNewValidatedBlock(Identifier blockId) throws IllegalStateException, IllegalArgumentException {
    this.logger.info("block_id: {}, new validated block arrived", blockId.toString());

    Block retrievedBlock = blocks.byId(blockId);
    if (retrievedBlock == null) {
      this.logger.error("validated block is not in database");
      throw new IllegalArgumentException("block is not in database");
    }

    try {
      lock.lock();

      // sanity checks that there is no pending last proposed block at this node
      BlockProposal lastProposedBlock = this.blockProposals.getLastProposal();
      if (lastProposedBlock != null) {
        throw new IllegalStateException("received validated block while having a pending proposed one, "
            + " received_block_id: " + blockId
            + " pending_block_id: " + lastProposedBlock.id());
      }

      Identifier nextBlockBlockProposerId = this.proposerAssigner.nextBlockProposer(blockId, state.atBlockId(blockId));
      if (!nextBlockBlockProposerId.equals(this.local.myId())) {
        // this node is not proposer of the next block.
        this.logger.debug("node_id: {}, current_block_id: {}, next_block_proposer_id: {}, node is not the proposer of next block",
            this.local.myId(), blockId, nextBlockBlockProposerId);
        return;
      }

      this.logger.debug("node_id: {}, current_block_id: {}, node is the proposer of next block",
          this.local.myId(), blockId);
      while (!checkPendingTransactionsWithBackoff(Parameters.MIN_TRANSACTIONS_NUM, 5000)) {
        this.logger.debug("busy waiting for enough pending transactions");
      }

      ValidatedTransaction[] transactions = this.collectTransactionsForBlock(Parameters.MIN_TRANSACTIONS_NUM);
      this.logger.debug("total_transactions: {}, transactions_ids {}, pending validated transactions collected for the next block",
          transactions.length, Convert.identifierOf(transactions));

      BlockPayload nextBlockPayload = new BlockPayload(transactions);
      BlockHeader nextBlockHeader = new BlockHeader(
          retrievedBlock.getHeight() + 1,
          retrievedBlock.id(),
          local.myId(),
          nextBlockPayload.id());
      Signature proposerSignature = local.signEntity(nextBlockHeader);
      BlockProposal nextProposedBlock = new BlockProposal(nextBlockHeader, nextBlockPayload, proposerSignature);
      this.logger.info("block_id: {}, processed next block successfully", nextProposedBlock.id());

      Assignment validators = this.validatorAssigner.getValidatorsAtSnapshot(nextProposedBlock.id(), this.state.atBlockId(blockId));

      // Sends block to validators.
      for (Identifier id : validators.all()) {
        try {
          // TODO: replace with multicast.
          this.proposerCon.unicast(nextProposedBlock, id);
          this.logger.debug("block_id {}, validator_id {}, proposal sent for validation", nextProposedBlock.id(), id);
        } catch (LightChainNetworkingException e) {
          throw new IllegalStateException("could not unicast new block for validation", e);
        }
      }

      this.blockProposals.setLastProposal(nextProposedBlock);
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
    BlockApproval approval = (BlockApproval) e;
    this.logger.info("block_id: {}, signer_id: {}, block approval arrived", approval.blockId, approval.getSignature().getSignerId());

    try {
      this.lock.lock();
      // sanity checks that there is no pending last proposed block at this node
      BlockProposal lastBlockProposal = this.blockProposals.getLastProposal();
      if (lastBlockProposal == null) {
        throw new IllegalStateException("received block approval while there is no last proposed block, approval id: " + approval.blockId);
      }

      if (!lastBlockProposal.id().equals(approval.blockId)) {
        throw new IllegalStateException("conflicting block approval id, last proposed block: " + lastBlockProposal.id()
            + " approval id: " + approval.blockId);
      }
      // TODO: verify signature of the approval and the fact that it is coming from an assigned validator before adding to database.
      approvals.add(approval);
      if (approvals.size() >= Parameters.VALIDATOR_THRESHOLD) {
        Signature[] certificates = new Signature[Parameters.VALIDATOR_THRESHOLD];
        for (int i = 0; i < approvals.size(); i++) {
          certificates[i] = approvals.get(i).getSignature();
        }
        Block nextBlock = new Block(lastBlockProposal, certificates);
        Snapshot snapshot = state.atBlockId(lastBlockProposal.getPreviousBlockId());

        for (Account account : snapshot.all()) {
          try {
            // TODO: replace with broadcast.
            validatedCon.unicast(nextBlock, account.getIdentifier());
          } catch (LightChainNetworkingException ex) {
            // TODO: switch to FATAL.
            this.logger.error("target_id {}, exception {}, could not send block to target",
                account.getIdentifier(), Arrays.toString(ex.getStackTrace()));
          }
        }
        approvals.clear();
        this.blockProposals.clearLastProposal();
        this.logger.info("block_id: {}, next proposed and validated block has been sent to all nodes", nextBlock.id());
      }
    } finally {
      this.lock.unlock();
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
    if (pendingTransactions.size() < count) {
      // TODO: optimize busy wating
      // Waits until there are enough pending transactions.
      this.logger.warn("minimum_required_transactions {}, total_pending_transactions {}, waiting for enough pending transactions",
          Parameters.MIN_TRANSACTIONS_NUM, pendingTransactions.size());
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