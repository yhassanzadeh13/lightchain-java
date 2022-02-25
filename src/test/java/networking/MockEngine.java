package networking;

import java.util.Set;

import model.Entity;
import model.lightchain.Identifier;
import protocol.Engine;

public class MockEngine implements Engine {
  private Set<Identifier> receivedEntityIds;

  @Override
  public void process(Entity e) throws IllegalArgumentException {
    // TODO: put e.Id() in the set.
  }
}
