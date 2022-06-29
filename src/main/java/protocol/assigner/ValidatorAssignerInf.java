package protocol.assigner;

import model.lightchain.Assignment;
import model.lightchain.Identifier;
import state.Snapshot;

public interface ValidatorAssigner {
  Assignment getValidatorsAtSnapshot(Identifier identifier, Snapshot snapshot) throws IllegalStateException;
}
