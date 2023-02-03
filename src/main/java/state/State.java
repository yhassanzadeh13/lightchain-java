package state;

import model.lightchain.Block;
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
   * Adds snapshot to state.
   *
   * @param blockId  identifier of block representing snapshot.
   * @param snapshot the snapshot associated with block
   * @throws IllegalStateException if a snapshot is already associated with block id.
   */
  void addSnapshot(Identifier blockId, Snapshot snapshot) throws IllegalStateException;

  /**
   * The most recent finalized state snapshot (tail of the snapshot list).
   *
   * @return the most recent finalized state snapshot of the node. Note that it never returns a null, since at the
   *     bare minimum the snapshot of Genesis block exists.
   */
  Snapshot last();

  /**
   * Executes the block by creating a new snapshot, applying all transactions on it, and then storing that snapshot
   * in the state and updating the last.
   *
   * @param block block to be executed.
   * @return snapshot resulted by executing the block.
   * @throws IllegalStateException if any illegal state faced during execution of a block.
   */
  Snapshot execute(Block block) throws IllegalStateException;
}
