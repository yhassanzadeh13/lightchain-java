package protocol.assigner;

import model.lightchain.Assignment;
import model.lightchain.Identifier;
import state.Snapshot;

/**
 * Validator assigner abstracts the logic of assigning deterministic validators for a given entity.
 */
public interface ValidatorAssigner {
  /**
   * Returns the validators of the given entity.
   *
   * @param identifier identifier of entity that urges validator assignment.
   * @param snapshot   snapshot of protocol state from which validators are picked.
   * @return list of validators assigned to this entity.
   * @throws IllegalStateException any unhappy path taken on computing validators.
   */
  Assignment getValidatorsAtSnapshot(Identifier identifier, Snapshot snapshot) throws IllegalStateException;
}
