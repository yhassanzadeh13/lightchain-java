package protocol.assigner;

import model.lightchain.Assignment;
import model.lightchain.Identifier;
import state.Snapshot;

/**
 * Represents the abstraction of assignment for an entity.
 */
// TODO: rename to Selector?
public interface AssignerInf {
  /**
   * Picks identifiers from the given snapshot to the entity with given identifier.
   * Identifier of the ith selected identifier is chosen as the staked account with the greatest identifier that
   * is less than or equal to hash(id || i). Once the ith identifier is chosen, it is omitted from the procedure
   * of picking the i+1(th) one.
   *
   * @param id identifier of the entity.
   * @param s snapshot to select identifiers from.
   * @param num number of identifiers to select.
   * @return list of selected identifiers.
   */
  Assignment assign(Identifier id, Snapshot s, short num) throws IllegalArgumentException;
}
