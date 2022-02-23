package state;

import model.lightchain.Identifier;

/**
 * State represents the full LightChain protocol state of the local node. It allows us to
 * obtain snapshots of the state at any point of the protocol state history.
 */
public interface State {
  /**
   * Fetches snapshot at the given finalized block id.
   *
   * @param identifier identifier of corresponding block for snapshot.
   * @return the snapshot of the local node at the given finalized block id, or null of such snapshot does not exist.
   */
  Snapshot atBlockId(Identifier identifier);

  /**
   * The most recent finalized state snapshot (tail of the snapshot list).
   *
   * @return the most recent finalized state snapshot of the node. Note that it never returns a null, since at the
   * bare minimum the snapshot of Genesis block exists.
   */
  Snapshot last();
}
