package protocol.assigner;

import model.lightchain.Assignment;
import model.lightchain.Identifier;
import state.Snapshot;

/**
 * Represents the abstraction of assignment of validators to an entity.
 */
public interface ValidatorAssigner {
  /**
   * Assigns validators from the given snapshot to the entity with given identifier.
   *
   * @param id identifier of the entity.
   * @param s snapshot to pick validators from.
   * @param num number of validators to choose.
   * @return list of validators.
   */
  Assignment assign(Identifier id, Snapshot s, short num);
}
