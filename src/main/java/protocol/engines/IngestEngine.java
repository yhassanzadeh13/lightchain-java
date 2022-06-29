package protocol.engines;

import java.util.concurrent.locks.ReentrantLock;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import model.Entity;
import model.codec.EntityType;
import model.crypto.Signature;
import model.lightchain.*;
import protocol.Engine;
import protocol.Parameters;
import protocol.assigner.AssignerInf;
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
  private final AssignerInf assigner;

  /**
   * Constructor of a IngestEngine.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "fields are intentionally mutable externally")
  public IngestEngine(State state,
                      Blocks blocks,
                      Identifiers transactionIds,
                      Transactions pendingTransactions,
                      Identifiers seenEntities,
                      AssignerInf assigner) {
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
        this.handleValidatedBlock(block, certificates);

      } else if (e.type().equals(EntityType.TYPE_VALIDATED_TRANSACTION)) {
        Transaction tx = ((Transaction) e); // skims off the non-transaction attributes (e.g., certificates).
        Signature[] certificates = ((ValidatedTransaction) e).getCertificates();
        this.handleValidatedTransaction(tx, certificates);
      }

      seenEntities.add(e.id());
    } finally {
      lock.unlock();
    }
  }

  /**
   * Handles a supposedly validated transaction given its set of certificates.
   *
   * @param tx           incoming transaction.
   * @param certificates set of transaction signatures.
   */
  private void handleValidatedTransaction(Transaction tx, Signature[] certificates) {
    // performs validator assignment.
    Snapshot snapshot = this.state.atBlockId(tx.getReferenceBlockId());
    if (!this.validateCertificatesForEntity(tx, snapshot, certificates)) {
      return;
    }

    if (pendingTransactions.has(tx.id())) {
      return;
    }

    if (transactionIds.has(tx.id())) {
      return;
    }

    // transaction has not seen before, and it is not pending either.
    pendingTransactions.add(tx);
  }

  /**
   * Handles a supposedly validated block given its set of certificates.
   *
   * @param block        incoming block.
   * @param certificates set of block certificates.
   */
  private void handleValidatedBlock(Block block, Signature[] certificates) {
    // performs validator assignment.
    Snapshot snapshot = this.state.atBlockId(block.getPreviousBlockId());
    if (!this.validateCertificatesForEntity(block, snapshot, certificates)) {
      return;
    }

    if (blocks.has(block.id())) {
      return;
    }

    blocks.add(block);

    for (ValidatedTransaction t : block.getTransactions()) {
      transactionIds.add(t.id());
      if (pendingTransactions.has(t.id())) {
        pendingTransactions.remove(t.id());
      }
    }
  }

  /**
   * Validates the certificates for given entity through performing a validator assignment and checking signature of
   * assigned validators against given snapshot.
   *
   * @param entity       the entity to verify.
   * @param snapshot     snapshot at which public key of validators are taken.
   * @param certificates list of validators signatures over the entity.
   * @return true if entity has enough signatures from its assigned validators, false otherwise.
   */
  private boolean validateCertificatesForEntity(Entity entity, Snapshot snapshot, Signature[] certificates) {
    Assignment assignment = this.assigner.assign(entity.id(), snapshot, Parameters.VALIDATOR_THRESHOLD);

    int signatures = 0;
    for (Signature certificate : certificates) {
      if (!assignment.has(certificate.getSignerId())) {
        // TODO: add a return value and log the reason.
        // certificate issued by a non-assigned validator
        return false;
      }
      if (snapshot
          .getAccount(certificate.getSignerId())
          .getPublicKey()
          .verifySignature(entity, certificate)) {
        signatures++;
      }
    }

    return signatures >= Parameters.SIGNATURE_THRESHOLD;
  }
}
