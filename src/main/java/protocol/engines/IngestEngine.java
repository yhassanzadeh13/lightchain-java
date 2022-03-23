package protocol.engines;

import model.Entity;
import model.lightchain.Block;
import model.codec.EntityType;
import model.lightchain.Transaction;
import model.lightchain.ValidatedTransaction;
import protocol.Engine;
import protocol.block.BlockVerifier;
import protocol.transaction.TransactionVerifier;
import state.State;
import storage.Blocks;
import storage.Transactions;
import storage.Identifiers;

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
    if (e.type() == EntityType.TYPE_BLOCK) {
      if (isBlockVerified((Block) e)) {
        for (ValidatedTransaction t : ((Block) e).getTransactions()) {
          transactionIds.add(t.id());
          if (pendingTransactions.has(t.id())) {
            pendingTransactions.remove(t.id());
          }
        }
      }
    } else if (e.type() == EntityType.TYPE_TRANSACTION) {
      if (transactionIds.has(e.id())) {
        transactionIds.remove(e.id());
      } else if (isTransactionVerified((Transaction) e)) {
        pendingTransactions.add((Transaction) e);
      }
    } else {
      throw new IllegalArgumentException("entity is neither a transaction nor a block");
    }
  }

  public boolean isBlockVerified(Block b) {
    BlockVerifier verifier = new BlockVerifier(state);
    return verifier.allTransactionsSound(b) && verifier.allTransactionsValidated(b) && verifier.isAuthenticated(b)
        && verifier.isConsistent(b) && verifier.isCorrect(b) && verifier.noDuplicateSender(b) && verifier.proposerHasEnoughStake(b);
  }

  public boolean isTransactionVerified(Transaction t) {
    TransactionVerifier verifier = new TransactionVerifier(state);
    return verifier.isSound(t) && verifier.senderHasEnoughBalance(t) && verifier.isAuthenticated(t) && verifier.isCorrect(t);
  }
}
