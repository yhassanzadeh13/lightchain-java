package networking;

import java.util.concurrent.ConcurrentHashMap;

import network.Conduit;
import network.Network;
import protocol.Engine;

public class StubNetwork implements Network {
  private ConcurrentHashMap<String, Engine> engines;
  private Hub hub;

  public StubNetwork(Hub hub){
    this.engines = new ConcurrentHashMap<>();
    this.hub = hub;
  }

  @Override
  public Conduit register(Engine e, String channel) throws IllegalStateException {
    return null;
  }
}
