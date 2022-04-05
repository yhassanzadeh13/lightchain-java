package state.table;

import java.util.Hashtable;

import model.lightchain.Identifier;
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
   * @param blockId identifier of block representing snapshot.
   * @param snapshot the snapshot associated with block
   * @throws IllegalStateException if a snapshot is already associated with block id.
   */
  public void addSnapshot(Identifier blockId, Snapshot snapshot) throws IllegalStateException {
    if (this.table.get(blockId) != null){
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
    long maxHeight = 0L;
    // TODO: this linear search can be optimized further.
    for (Snapshot snapshot : this.table.values()) {
      if (snapshot.getReferenceBlockHeight() > maxHeight) {
        lastBlockId = snapshot.getReferenceBlockId();
        maxHeight = snapshot.getReferenceBlockHeight();
      }
    }
    return this.table.get(lastBlockId);
  }
}
