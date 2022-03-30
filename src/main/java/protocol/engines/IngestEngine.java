package protocol.engines;

import model.Entity;
import protocol.Engine;

/**
 * The ingest engine is responsible for receiving new transactions and blocks from the network, and organizing them
 * into the transactions and blocks storage pools.
 */
public class IngestEngine implements Engine {
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
   * further processing.
   * -----
   *
   * @param e the arrived Entity from the network, it should be either a transaction or a block.
   * @throws IllegalArgumentException when the arrived entity is neither a transaction nor a block.
   */
  @Override
  public void process(Entity e) throws IllegalArgumentException {
  }
}
