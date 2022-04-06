package protocol.engines;

import model.Entity;
import model.lightchain.Block;
import model.codec.EntityType;
import model.lightchain.Transaction;
import model.lightchain.ValidatedBlock;
import model.lightchain.ValidatedTransaction;
import protocol.Engine;
import protocol.Parameters;
import protocol.block.BlockValidator;
import protocol.transaction.TransactionValidator;
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
  public static Blocks blocks;
  public static Identifiers transactionIds;
  public static Transactions pendingTransactions;
  /**
   * Unique State that the block is in.
   */
  private final State state;

  /**
   * Constructor of a IngestEngine.
   */
  public IngestEngine(State state) {
    this.state = state;
  }

  private static ReentrantLock lock = new ReentrantLock();

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
    //TODO: should a tryLock be used here?
    lock.lock();
    if (e.type() == EntityType.TYPE_VALIDATED_BLOCK) {
      if (((ValidatedBlock) e).getCertificates().length >= Parameters.SIGNATURE_THRESHOLD) {
        blocks.add((Block) e);
        for (ValidatedTransaction t : ((Block) e).getTransactions()) {
          transactionIds.add(t.id());
          if (pendingTransactions.has(t.id())) {
            pendingTransactions.remove(t.id());
          }
        }
      }
    } else if (e.type() == EntityType.TYPE_VALIDATED_TRANSACTION) {
      if (((ValidatedTransaction) e).getCertificates().length >= Parameters.SIGNATURE_THRESHOLD) {
        if (transactionIds.has(e.id())) {
          transactionIds.remove(e.id());
        } else {
          pendingTransactions.add((Transaction) e);
        }
      }
    } else {
      lock.unlock();
      throw new IllegalArgumentException("entity is neither a validated transaction nor a validated block");
    }
    lock.unlock();
  }


}

