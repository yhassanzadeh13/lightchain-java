package state;

import model.lightchain.Identifier;

public interface State {
  Snapshot AtBlockId(Identifier identifier);
  Snapshot Final();
}
