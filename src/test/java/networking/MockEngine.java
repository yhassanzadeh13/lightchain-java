package networking;

import java.util.HashSet;
import java.util.Set;

import model.Entity;
import model.lightchain.Identifier;
import protocol.Engine;

public class MockEngine implements Engine {
  private Set<Identifier> receivedEntityIds;

  public MockEngine() {
    this.receivedEntityIds = new HashSet<>();
  }

  @Override
  public String toString() {
    return "MockEngine{" +
            "receivedEntityIds=" + receivedEntityIds +
            '}';
  }

  @Override
  public void process(Entity e) throws IllegalArgumentException {
    // TODO: put e.Id() in the set.
    receivedEntityIds.add(e.id());
  }
}