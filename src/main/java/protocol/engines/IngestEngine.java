package protocol.engines;

import model.Entity;
import model.lightchain.*;
import model.codec.EntityType;
import model.local.Local;
import protocol.Engine;
import protocol.Parameters;
import protocol.assigner.LightChainValidatorAssigner;
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
  public Identifiers seenEntities;//TODO: Add the seen entities


  /**
   * Constructor of a IngestEngine.
   */
  public IngestEngine() {
  }

  private static final ReentrantLock lock = new ReentrantLock();

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

    if (e.type() == EntityType.TYPE_VALIDATED_BLOCK || e.type() == EntityType.TYPE_VALIDATED_TRANSACTION) {
      lock.lock();
      LightChainValidatorAssigner assigner = new LightChainValidatorAssigner();
      // TODO: assigner.assign(e, snapshot???, how many???);
      // TODO: don't we need a method to check whether an id is in Assignment?


      if (e.type() == EntityType.TYPE_VALIDATED_BLOCK) {
        if (((ValidatedBlock) e).getCertificates().length >= Parameters.SIGNATURE_THRESHOLD
            && !blocks.has(((ValidatedBlock) e).id())) { //TODO: Ask whether this condition is true
          blocks.add((Block) e);
          for (ValidatedTransaction t : ((Block) e).getTransactions()) {
            transactionIds.add(t.id());
            if (pendingTransactions.has(t.id())) {
              pendingTransactions.remove(t.id());
            }
          }
        }
      } else if (e.type() == EntityType.TYPE_VALIDATED_TRANSACTION) {
        if (((ValidatedTransaction) e).getCertificates().length >= Parameters.SIGNATURE_THRESHOLD
            && !pendingTransactions.has(((ValidatedTransaction) e).id())) {
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


}

