package protocol.engines;

import model.Entity;
import model.lightchain.*;
import model.codec.EntityType;
import protocol.Engine;
import protocol.Parameters;
import protocol.assigner.LightChainValidatorAssigner;
import state.State;
import storage.Blocks;
import storage.Transactions;
import storage.Identifiers;

import java.util.concurrent.locks.ReentrantLock;


/**
 * The ingest engine is responsible for receiving new transactions and blocks from the network, and organizing them
 * into the transactions and blocks storage pools.
 */
public class IngestEngine implements Engine {
  private final State state;
  private final Blocks blocks;
  private final Identifiers transactionIds;
  private final Transactions pendingTransactions;
  private final Identifiers seenEntities;//TODO: Add the seen entities
  private final ReentrantLock lock = new ReentrantLock();


  /**
   * Constructor of a IngestEngine.
   */
  public IngestEngine(State state, Blocks blocks, Identifiers transactionIds, Transactions pendingTransactions, Identifiers seenEntities) {
    this.state = state;
    this.blocks = blocks;
    this.transactionIds = transactionIds;
    this.pendingTransactions = pendingTransactions;
    this.seenEntities = seenEntities;
  }

  /**
   * Received entity to this engine can be either a ValidatedBlock or a ValidatedTransaction,
   * anything else should throw an exception. Upon receiving a block or transaction,
   * the engine runs the assignment, fetches the list of validators for this entity, and checks whether
   * entity has enough signatures by its validators over its entity identifier.
   * -----
   * For a validated block with enough valid signatures, the engine adds the block to its block storage database.
   * The engine also adds HASH of all the transactions of block into its "txIds" database.
   * If any of these transactions already exists in the "pendingTx" database,
   * they will be removed from "pendingTx" (as they already included in a validated block).
   * -----
   * For a validated transaction with enough signatures, the engine first checks whether the transaction has already
   * been included in a block by looking for the hash of transaction into its "txIds" database.
   * If the transaction is already included in a block, the engine discards it. Otherwise, it is added to the
   * "pendingTx" database, and otherwise is discarded.
   * -----
   * Note that engine should always discard transactions and blocks that it has seen before without any further
   * processing.
   * -----
   *
   * @param e the arrived Entity from the network, it should be either a transaction or a block.
   * @throws IllegalArgumentException when the arrived entity is neither a transaction nor a block.
   */
  @Override
  public void process(Entity e) throws IllegalArgumentException {

    if (e.type().equals(EntityType.TYPE_VALIDATED_BLOCK) || e.type().equals(EntityType.TYPE_VALIDATED_TRANSACTION)) {
      lock.lock();
      LightChainValidatorAssigner assigner = new LightChainValidatorAssigner();
      if (e.type().equals(EntityType.TYPE_VALIDATED_BLOCK)) {
        ValidatedBlock b = ((ValidatedBlock) e);
        Assignment assignment = assigner.assign(b.id()
            , state.atBlockId(b.getPreviousBlockId())
            , (short) Parameters.VALIDATOR_THRESHOLD);

        int signatures = 0;
        for (Identifier id : assignment.getAll()) {
          if (this.state.atBlockId(b.getPreviousBlockId()).getAccount(id).getPublicKey().verifySignature(b, b.getSignature()) ){
            signatures++;
          }
        }
        if (!seenEntities.has(e.id()) && signatures >= Parameters.SIGNATURE_THRESHOLD
            && !blocks.has(b.id())) {
          blocks.add((Block) e);
          for (ValidatedTransaction t : b.getTransactions()) {
            transactionIds.add(t.id());
            if (pendingTransactions.has(t.id())) {
              pendingTransactions.remove(t.id());
            }
          }
        }
      } else if (e.type().equals(EntityType.TYPE_VALIDATED_TRANSACTION)) {
        Assignment assignment = assigner.assign(e.id()
            , state.atBlockId(((ValidatedTransaction) e).getReferenceBlockId())
            , (short) Parameters.VALIDATOR_THRESHOLD);
        int signatures = assignment.size();
        if (!seenEntities.has(e.id()) && signatures >= Parameters.SIGNATURE_THRESHOLD
            && !pendingTransactions.has(((ValidatedTransaction) e).id())) {
          if (!transactionIds.has(e.id())) {
            pendingTransactions.add((Transaction) e);
          }
        }
      }
      lock.unlock();
    } else {
      throw new IllegalArgumentException("entity is neither a validated transaction nor a validated block");
    }
  }


}

