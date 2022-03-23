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
  private Hashtable<Identifier, Snapshot> table;

  public TableState() {
    this.table = new Hashtable<>();
  }

  public void addSnapshot(Identifier blockId, Snapshot snapshot) {
    this.table.put(blockId, snapshot);
  }

  @Override
  public Snapshot atBlockId(Identifier identifier) {
    return this.table.get(identifier);
  }

  @Override
  public Snapshot last() {
    Identifier lastBlockId = null;
    long maxHeight = 0L;
    for (Snapshot snapshot : this.table.values()) {
      if (snapshot.getReferenceBlockHeight() > maxHeight) {
        lastBlockId = snapshot.getReferenceBlockId();
        maxHeight = snapshot.getReferenceBlockHeight();
      }
    }
    return this.table.get(lastBlockId);
  }
}
