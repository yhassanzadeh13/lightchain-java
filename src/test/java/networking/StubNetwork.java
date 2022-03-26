package networking;

import java.util.concurrent.ConcurrentHashMap;

import model.Entity;
import model.lightchain.Identifier;
import network.Conduit;
import network.Network;
import protocol.Engine;
import unittest.fixtures.IdentifierFixture;

/**
 * A mock implementation of networking layer as a test util.
 */
public class StubNetwork implements Network {
  private final ConcurrentHashMap<String, Engine> engines;
  private final Hub hub;
  private final Identifier identifier;

  /**
   * Create stubNetwork.
   *
   * @param hub the hub which stubnetwork registered is.
   */
  public StubNetwork(Hub hub) {
    this.engines = new ConcurrentHashMap<>();
    this.hub = hub;
    this.identifier = IdentifierFixture.newIdentifier();
    this.hub.registerNetwork(identifier, this);
  }

  /**
   * Get the identifier of the stubnet.
   *
   * @return identifier.
   */
  public Identifier id() {
    return this.identifier;
  }

  /**
   * Forward the incoming entity to the engine whose channel is given.
   *
   * @param entity  received entity
   * @param channel the channel through which the received entity is sent
   */
  public void receiveUnicast(Entity entity, String channel) {
    Engine engine = getEngine(channel);
    try {
      engine.process(entity);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("could not process the entity" + e);
    }
  }

  /**
   * Registers an Engine to the Network by providing it with a Conduit.
   *
   * @param en      the Engine to be registered.
   * @param channel the unique channel corresponding to the Engine.
   * @return unique Conduit object created to connect the Network to the Engine.
   * @throws IllegalStateException if the channel is already taken by another Engine.
   */
  @Override
  public Conduit register(Engine en, String channel) throws IllegalStateException {
    Conduit conduit = new MockConduit(channel, hub);
    try {
      if (engines.containsKey(channel)) {
        throw new IllegalStateException();
      }
      engines.put(channel, en);
    } catch (IllegalArgumentException ex) {
      throw new IllegalStateException("could not register the engine" + ex);
    }
    return conduit;
  }

  public Engine getEngine(String ch) {
    return engines.get(ch);
  }
}