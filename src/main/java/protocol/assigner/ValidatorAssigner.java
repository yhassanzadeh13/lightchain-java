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
   * Identifier of the ith validator is chosen as the staked account with the greatest identifier that
   * is less than or equal to hash(id || i). Once the ith validator is chosen, it is omitted from the procedure
   * of picking the i+1(th) validator.
   *
   * @param id  identifier of the entity.
   * @param s   snapshot to pick validators from.
   * @param num number of validators to choose.
   * @return list of validators.
   */
  Assignment assign(Identifier id, Snapshot s, short num) throws IllegalArgumentException;
}
