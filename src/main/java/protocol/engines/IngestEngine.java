package protocol.engines;

import java.util.concurrent.locks.ReentrantLock;

import model.Entity;
import model.codec.EntityType;
import model.crypto.Signature;
import model.lightchain.*;
import protocol.Engine;
import protocol.Parameters;
import protocol.assigner.ValidatorAssigner;
import state.Snapshot;
import state.State;
import storage.Blocks;
import storage.Identifiers;
import storage.Transactions;

/**
 * The ingest engine is responsible for receiving new transactions and blocks from the network, and organizing them
 * into the transactions and blocks storage pools.
 */
public class IngestEngine implements Engine {
  private final State state;
  private final Blocks blocks;
  private final Identifiers transactionIds;
  private final Transactions pendingTransactions;
  private final Identifiers seenEntities; //TODO: Add the seen entities
  private final ReentrantLock lock = new ReentrantLock();
  private final ValidatorAssigner assigner;

  /**
   * Constructor of a IngestEngine.
   */
  public IngestEngine(State state,
                      Blocks blocks,
                      Identifiers transactionIds,
                      Transactions pendingTransactions,
                      Identifiers seenEntities,
                      ValidatorAssigner assigner) {
    this.state = state;
    this.blocks = blocks;
    this.transactionIds = transactionIds;
    this.pendingTransactions = pendingTransactions;
    this.seenEntities = seenEntities;
    this.assigner = assigner;
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
    try {
      lock.lock();
      if (!e.type().equals(EntityType.TYPE_VALIDATED_BLOCK)
          && !e.type().equals(EntityType.TYPE_VALIDATED_TRANSACTION)) {
        throw new IllegalArgumentException("entity is neither a validated transaction nor a validated block");
      }

      if (seenEntities.has(e.id())) {
        return; // entity already ingested.
      }

      if (e.type().equals(EntityType.TYPE_VALIDATED_BLOCK)) {
        Block block = ((Block) e); // skims off the non-block attributes (e.g., certificates).
        Signature[] certificates = ((ValidatedBlock) e).getCertificates();

        // performs validator assignment.
        Snapshot snapshot = this.state.atBlockId(block.getPreviousBlockId());
        Assignment assignment = this.assigner.assign(block.id(), snapshot, Parameters.VALIDATOR_THRESHOLD);

        int signatures = 0;
        for (Signature certificate : certificates) {
          if (!assignment.has(certificate.getSignerId())) {
            // certificate issued by a non-assigned validator
            return;
          }
          if (snapshot
              .getAccount(certificate.getSignerId())
              .getPublicKey()
              .verifySignature(block, certificate)) {
            signatures++;
          }
        }

        if (signatures >= Parameters.SIGNATURE_THRESHOLD && !blocks.has(block.id())) {
          blocks.add(block);
          for (ValidatedTransaction t : block.getTransactions()) {
            transactionIds.add(t.id());
            if (pendingTransactions.has(t.id())) {
              pendingTransactions.remove(t.id());
            }
          }
        }

      } else if (e.type().equals(EntityType.TYPE_VALIDATED_TRANSACTION)) {
        Transaction tx = ((Transaction) e); // skims off the non-transaction attributes (e.g., certificates).
        Signature[] certificates = ((ValidatedTransaction) e).getCertificates();

        // performs validator assignment.
        Snapshot snapshot = this.state.atBlockId(tx.getReferenceBlockId());
        Assignment assignment = this.assigner.assign(tx.id(), snapshot, Parameters.VALIDATOR_THRESHOLD);

        int signatures = 0;
        for (Signature certificate : certificates) {
          if (!assignment.has(certificate.getSignerId())) {
            // certificate issued by a non-assigned validator
            return;
          }
          if (snapshot
              .getAccount(certificate.getSignerId())
              .getPublicKey()
              .verifySignature(tx, certificate)) {
            signatures++;
          }
        }

        if (signatures >= Parameters.SIGNATURE_THRESHOLD) {
          if (!transactionIds.has(tx.id())) {
            pendingTransactions.add(tx);
          }
        }
      }

      seenEntities.add(e.id());
    } finally {
      lock.unlock();
    }
  }
}



