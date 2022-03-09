package state.table;

import java.util.Hashtable;

import model.lightchain.Identifier;
import state.Snapshot;

/**
 * Implements a simplified hash table-based model of the protocol state.
 */
public class TableState implements state.State {
  private Hashtable<Identifier, Snapshot> table;

  @Override
  public Snapshot atBlockId(Identifier identifier) {
    return null;
  }

  @Override
  public Snapshot last() {
    return null;
  }
}
