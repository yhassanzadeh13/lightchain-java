package state.table;

import java.util.Hashtable;

import model.lightchain.Block;
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

  // TODO: implement add method to add snapshot at given block id.

  public TableState() {
    this.table = new Hashtable<>();
  }

  @Override
  public Snapshot atBlockId(Identifier identifier) {
    return table.get(identifier);
  }

  @Override
  public Snapshot last() {
    return null;
  }

  @Override
  public Snapshot execute(Block block) throws IllegalStateException {
    return null;
  }
}
