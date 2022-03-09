package state.table;

import java.util.Hashtable;

import model.lightchain.Account;
import model.lightchain.Identifier;

/**
 * Implements a simplified hash-table based model of the protocol snapshot at a given block.
 */
public class TableSnapshot implements state.Snapshot {
  private final Identifier rootBlockId;
  private final long rootBlockHeight;

  private Hashtable<Identifier, Account> table;

  public TableSnapshot(Identifier rootBlockId, long rootBlockHeight) {
    this.rootBlockId = rootBlockId;
    this.rootBlockHeight = rootBlockHeight;
  }

  @Override
  public Identifier getReferenceBlockId() {
    return rootBlockId;
  }

  @Override
  public long getReferenceBlockHeight() {
    return rootBlockHeight;
  }

  @Override
  public Account getAccount(Identifier identifier) {
    return table.get(identifier);
  }
}
