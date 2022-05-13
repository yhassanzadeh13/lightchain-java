package protocol.engines;

import model.Entity;
import model.codec.EntityType;
import model.crypto.KeyGen;
import model.crypto.PrivateKey;
import model.crypto.Signature;
import model.crypto.ecdsa.EcdsaKeyGen;
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
import state.State;
import storage.Blocks;
import storage.Transactions;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Proposer engine encapsulates the logic of creating new blocks.
 */
public class ProposerEngine implements NewBlockSubscriber, Engine {
  private static Local local;
  private static Blocks blocks;
  private static Transactions pendingTransactions;
  private static State state;
  private static Conduit proposerCon;
  private static Conduit validatedCon;
  private static Network net;
  private ArrayList<BlockApproval> approvals;
  public Block newB;

  private static final ReentrantLock lock = new ReentrantLock();

  public ProposerEngine(Blocks blocks, Transactions pendingTransactions, State state, Local local, Network net) {
    this.local = local;
    this.blocks = blocks;
    this.pendingTransactions = pendingTransactions;
    this.state = state;
    this.approvals = new ArrayList<>();
    this.proposerCon = net.register(this, Channels.ProposedBlocks);
    this.validatedCon = net.register(this, Channels.ValidatedBlocks);
    this.net = net;
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
  public void onNewValidatedBlock(int blockHeight, Identifier blockId) throws IllegalStateException,
      IllegalArgumentException {

    if (lock.isLocked()) {
      throw new IllegalStateException("proposer engine is already running.");
    }
    lock.lock();

    // Adds the Block Proposer tag to the assigner.
    byte[] bytesId = blockId.getBytes();
    byte tag = Integer.valueOf(Tags.BlockProposerTag).byteValue();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    output.write(bytesId, 0, 32);
    output.write(tag);
    Identifier taggedId = new Identifier(output.toByteArray());

    // Calls the assigner with tagged id.
    LightChainValidatorAssigner assigner = new LightChainValidatorAssigner();
    Assignment assignment = assigner.assign(taggedId, state.atBlockId(blockId), (short) 1);

    // Checks whether the assigner has assigned this node.
    if (assignment.has(local.myId())) {

      // Waits until there are enough pending transactions.
      while (pendingTransactions.size() < Parameters.MIN_VALIDATED_TRANSACTIONS_NUM) {
      }

      ValidatedTransaction[] transactions = new ValidatedTransaction[Parameters.MIN_VALIDATED_TRANSACTIONS_NUM];
      for (int i = 0; i < Parameters.MIN_VALIDATED_TRANSACTIONS_NUM; i++) {
        Transaction tx = pendingTransactions.all().get(i);
        transactions[i] = ((ValidatedTransaction) tx);
        pendingTransactions.remove(tx.id());
      }

      Block newBlock = new Block(blockId, local.myId(), blockHeight + 1, transactions);
      newB = newBlock;
      ;
      Signature sign = local.signEntity(newBlock);
      newBlock.setSignature(sign);

      // Adds the Block Proposer tag to the assigner.
      bytesId = newBlock.id().getBytes();
      tag = Integer.valueOf(Tags.ValidatorTag).byteValue();
      output = new ByteArrayOutputStream();
      output.write(bytesId, 0, 32);
      output.write(tag);
      taggedId = new Identifier(output.toByteArray());

      assignment = assigner.assign(taggedId, state.atBlockId(newBlock.getPreviousBlockId()), (short) Parameters.VALIDATOR_THRESHOLD);
      for (Identifier id : assignment.all()) {
        try {
          this.proposerCon.unicast(newBlock, id);
        } catch (LightChainNetworkingException e) {
          e.printStackTrace();
        }
      }
    }
    lock.unlock();
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
    if (e.type() == EntityType.TYPE_BLOCK_APPROVAL) {
      if (((BlockApproval) e).getBlockId() == null) {
        approvals.add((BlockApproval) e);

      }
      while (approvals.size() < Parameters.VALIDATOR_THRESHOLD) {
      }

      Signature[] signs = new Signature[Parameters.VALIDATOR_THRESHOLD];
      for (int i = 0; i < approvals.size(); i++) {
        signs[i] = approvals.get(i).getSignature();
      }
      ValidatedBlock vBlock = new ValidatedBlock(newB.getPreviousBlockId()
          , newB.getProposer()
          , newB.getTransactions()
          , this.local.signEntity(newB)
          , signs,newB.getHeight());
      for (Map.Entry<Identifier, String> pair : ((P2pNetwork) net).getIdToAddressMap().entrySet()) {
        if (pair.getValue().equals(Channels.ValidatedBlocks)) {
          try {
            this.validatedCon.unicast(vBlock, pair.getKey());
          } catch (LightChainNetworkingException e1) {
            e1.printStackTrace();
          }
        }
      }
      approvals.clear();
      newB = null;
    } else {
      throw new IllegalArgumentException("entity is not of type BlockApproval");
    }

  }
}
