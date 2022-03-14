package networking;

import model.Entity;
import model.exceptions.LightChainNetworkingException;
import model.lightchain.Identifier;
import network.Conduit;
import network.Network;
import protocol.Engine;
import unittest.fixtures.IdentifierFixture;

import java.util.concurrent.ConcurrentHashMap;

public class StubNetwork implements Network {
  private final ConcurrentHashMap<String, Engine> engines;
  private final ConcurrentHashMap<String, Conduit> conduits;
  private final Hub hub;
  private final Identifier identifier;


  public StubNetwork(Hub hub) {
    this.engines = new ConcurrentHashMap<>();
    this.conduits = new ConcurrentHashMap<>();
    this.hub = hub;
    this.identifier = IdentifierFixture.NewIdentifier();
    this.hub.registerNetwork(identifier, this);
  }

  public Identifier id() {
    return this.identifier;
  }

  private void sendUnicast(String ch, StubNetwork stubNetworkR, Entity entity) throws LightChainNetworkingException {
    Conduit conduit = conduits.get(ch);
    conduit.unicast(entity, stubNetworkR.id());
  }

  @Override
  public MockConduit register(Engine en, String channel) throws IllegalStateException {
    // TODO: this should be a separate class.
    Conduit conduit = new MockConduit(channel,en, hub);
    try {
      if (engines.containsKey(channel)) {
        throw new IllegalStateException();
      }

      engines.put(channel, en);

      conduits.put(channel, conduit);
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return (MockConduit) conduit;
  }
    public Engine  getEngine(String ch){
    Engine engine = engines.get(ch);
    return engine;
  }

}