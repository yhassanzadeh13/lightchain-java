package protocol.engines;

import model.Entity;
import model.crypto.KeyGen;
import model.crypto.PrivateKey;
import model.crypto.Signature;
import model.crypto.ecdsa.EcdsaKeyGen;
import model.lightchain.*;
import model.local.Local;
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

/**
 * Proposer engine encapsulates the logic of creating new blocks.
 */
public class ProposerEngine implements NewBlockSubscriber, Engine {
  public static Local local;
  public static Blocks blocks;
  public static Transactions pendingTransactions;
  public static State state;

  public ProposerEngine(Blocks blocks, Transactions pendingTransactions, State state, Local local) {
    // TODO: Probably will be changed to shared storage component.
    this.local = local;
    this.blocks = blocks;
    this.pendingTransactions = pendingTransactions;
    this.state = state;
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

    //TODO: Change it accordingly so that it can share storage component with ingest engine (multithreading).
    if (assignment.has(local.myId()) && pendingTransactions.size()== Parameters.MIN_VALIDATED_TRANSACTIONS_NUM){

      ValidatedTransaction[] transactions = new ValidatedTransaction[Parameters.MIN_VALIDATED_TRANSACTIONS_NUM];
      for (int i = 0; i < Parameters.MIN_VALIDATED_TRANSACTIONS_NUM; i++) {
        Transaction tx = pendingTransactions.all().get(i);
        transactions[i]=((ValidatedTransaction) tx);
        pendingTransactions.remove(tx.id());
      }
      PrivateKey pk = new EcdsaKeyGen().getPrivateKey();
      // TODO: This constructor is newly added and sets sign to null.
      Block newBlock = new Block(blockId, local.myId(), blockHeight+1, transactions);
      Signature sign = pk.signEntity(newBlock);
      // TODO: can't signEntity without creating the entity itself, and cant create entity without signature
      // Kind of in a loop? signature might be set late but both has changes and signature is final attribute.

      // Adds the Block Proposer tag to the assigner.
      bytesId = newBlock.id().getBytes();
      tag = Integer.valueOf(Tags.ValidatorTag).byteValue();
      output = new ByteArrayOutputStream();
      output.write(bytesId, 0, 32);
      output.write(tag);
      taggedId = new Identifier(output.toByteArray());
      // TODO: Is it the newBlockid or blockId?
      assignment = assigner.assign(taggedId, state.atBlockId(newBlock.id()), (short) Parameters.VALIDATOR_THRESHOLD);
      int signatures = 0;
      for (Identifier id : assignment.all()) {
        // TODO: Sends it to validators.
      }

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

  }
}
