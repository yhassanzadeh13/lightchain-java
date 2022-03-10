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
  private final Hashtable<Identifier, Account> table;

  /**
   * Constructor of TableSnapShot.
   *
   * @param rootBlockId root block id representing this snapshot.
   * @param rootBlockHeight root block height of this snapshot.
   * @param table table of accounts.
   */
  public TableSnapshot(Identifier rootBlockId, long rootBlockHeight, Hashtable<Identifier, Account> table) {
    this.rootBlockId = rootBlockId;
    this.rootBlockHeight = rootBlockHeight;
    this.table = table;
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
