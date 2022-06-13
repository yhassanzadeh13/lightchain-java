package state.table;

import java.util.ArrayList;
import java.util.Hashtable;

import model.lightchain.Account;
import model.lightchain.Block;
import model.lightchain.Identifier;
import model.lightchain.ValidatedTransaction;
import state.Snapshot;

/**
 * Implements a simplified hash table-based model of the protocol state.
 */
public class TableState implements state.State {
  /**
   * Table of root block id as the key, and snapshot at the root block id as the value.
   */
  private final Hashtable<Identifier, Snapshot> table;

  public TableState() {
    this.table = new Hashtable<>();
  }

  /**
   * Adds snapshot to state.
   *
   * @param blockId  identifier of block representing snapshot.
   * @param snapshot the snapshot associated with block
   * @throws IllegalStateException if a snapshot is already associated with block id.
   */
  public void addSnapshot(Identifier blockId, Snapshot snapshot) throws IllegalStateException {
    if (this.table.get(blockId) != null) {
      throw new IllegalStateException("a snapshot for block id already exists: " + blockId.toString());
    }
    this.table.put(blockId, snapshot);
  }

  /**
   * Fetches snapshot at the given finalized block id.
   *
   * @param identifier identifier of corresponding block for snapshot.
   * @return the snapshot of the local node at the given finalized block id, or null of such snapshot does not exist.
   */
  @Override
  public Snapshot atBlockId(Identifier identifier) {
    return this.table.get(identifier);
  }

  /**
   * The most recent finalized state snapshot (tail of the snapshot list).
   *
   * @return the most recent finalized state snapshot of the node. Note that it never returns a null, since at the
   * bare minimum the snapshot of Genesis block exists.
   */
  @Override
  public Snapshot last() {
    Identifier lastBlockId = null;
    long maxHeight = -1L;
    // TODO: this linear search can be optimized further.
    for (Snapshot snapshot : this.table.values()) {
      if (snapshot.getReferenceBlockHeight() > maxHeight) {
        lastBlockId = snapshot.getReferenceBlockId();
        maxHeight = snapshot.getReferenceBlockHeight();
      }
    }
    return this.table.get(lastBlockId);
  }

  @Override
  public Snapshot execute(Block block) throws IllegalStateException {
    if (block == null) {
      throw new IllegalStateException("block is null");
    }
    if (block.getHeight()<= this.last().getReferenceBlockHeight()){
      throw new IllegalStateException("block height is less than or equal to the height of the last snapshot");
    }
    ArrayList<Account> accounts = this.last().all();
    for (ValidatedTransaction tx: block.getTransactions()){
      try {
        Account sender = accounts.get(accounts.indexOf(this.last().getAccount(tx.getSender())));
        sender.setBalance(sender.getBalance() - tx.getAmount());
        Account receiver = accounts.get(accounts.indexOf(this.last().getAccount(tx.getReceiver())));
        receiver.setBalance(receiver.getBalance() + tx.getAmount());
      }catch (IllegalStateException e){
        e.printStackTrace();
        throw new IllegalStateException("unexpected error while executing block", e);
      }
    }
    TableSnapshot snapshot = new TableSnapshot(block.id(), block.getHeight());
    for (Account account : accounts) {
      snapshot.addAccount(account.getIdentifier(), account);
    }
    this.addSnapshot(block.id(), snapshot);
    return snapshot;
  }
}
