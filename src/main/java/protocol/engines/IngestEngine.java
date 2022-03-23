package protocol.engines;

import model.Entity;
import protocol.Engine;

/**
 * The ingest engine is responsible for receiving new transactions and blocks from the network, and organizing them
 * into the transactions and blocks storage pools.
 */
public class IngestEngine implements Engine {
  /**
   * If received entity is a block, the engine runs block validation on it, and if it passes the validation,
   * the engine adds the block to its block storage database. The engine also adds HASH of all the transactions of
   * block into its "txHash" database. If any of these transactions already exists in the "pendingTx" database,
   * they will be removed from "pendingTx" (as they already included in a validated block).
   * If received entity is a transaction, the engine first checks whether the transaction has already
   * been included in a block by looking for the hash of transaction into its "txHash" database.
   * If the transaction is already included in a block, the engine discards it.
   * Otherwise, it runs the transaction validation on it. If the transaction passes validation, it is added to the
   * "pendingTx" database, and otherwise is discarded.
   *
   * @param e the arrived Entity from the network, it should be either a transaction or a block.
   * @throws IllegalArgumentException when the arrived entity is neither a transaction nor a block.
   */
  @Override
  public void process(Entity e) throws IllegalArgumentException {
  }
}
