package networking;

import java.util.concurrent.ConcurrentHashMap;

import network.Conduit;
import network.Network;
import protocol.Engine;

/**
 * A mock implementation of networking layer as a test util.
 */
public class StubNetwork implements Network {
  private final ConcurrentHashMap<String, Engine> engines;
  private final Hub hub;

  public StubNetwork(Hub hub) {
    this.engines = new ConcurrentHashMap<>();
    this.hub = hub;
  }

  @Override
  public Conduit register(Engine e, String channel) throws IllegalStateException {
    return null;
  }
}
