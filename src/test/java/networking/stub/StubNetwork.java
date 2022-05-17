package networking.stub;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import model.Entity;
import model.exceptions.LightChainDistributedStorageException;
import model.exceptions.LightChainNetworkingException;
import model.lightchain.Identifier;
import network.Conduit;
import network.Network;
import network.NetworkAdapter;
import networking.MockConduit;
import protocol.Engine;
import unittest.fixtures.IdentifierFixture;

/**
 * A mock implementation of networking layer as a test util.
 */
public class StubNetwork implements Network, NetworkAdapter {
  private final String channel1 = "test-network-channel-1";
  private final String channel2 = "test-network-channel-2";
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
  public void receiveUnicast(Entity entity, String channel) throws IllegalArgumentException {
    Engine engine = getEngine(channel);
    try {
      engine.process(entity);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("could not process the entity", e);
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
    Conduit conduit = new MockConduit(channel, this);
    try {
      if (engines.containsKey(channel)) {
        throw new IllegalStateException();
      }
      engines.put(channel, en);
    } catch (IllegalArgumentException ex) {
      throw new IllegalStateException("could not register the engine");
    }
    return conduit;
  }

  public Engine getEngine(String ch) {
    return engines.get(ch);
  }

  /**
   * Sends the Entity through the Network to the remote target.
   *
   * @param e       the Entity to be sent over the network.
   * @param target  Identifier of the receiver.
   * @param channel channel on which this entity is sent.
   * @throws LightChainNetworkingException any unhappy path taken on sending the Entity.
   */
  @Override
  public void unicast(Entity e, Identifier target, String channel) throws LightChainNetworkingException {
    try {
      this.hub.transferEntity(e, target, channel);
    } catch (IllegalStateException ex) {
      throw new LightChainNetworkingException("stub network could not transfer entity", ex);
    }
  }

  /**
   * Stores given Entity on the underlying Distributed Hash Table (DHT) of nodes.
   *
   * @param e         the Entity to be stored over the network.
   * @param namespace namespace on which this entity is stored.
   * @throws LightChainDistributedStorageException any unhappy path taken on storing the Entity.
   */
  @Override
  public void put(Entity e, String namespace) throws LightChainDistributedStorageException {
    this.hub.putEntityToChannel(e, namespace);
  }

  /**
   * Retrieves the entity corresponding to the given identifier form the underlying Distributed Hash Table
   * (DHT) of nodes.
   *
   * @param identifier identifier of the entity to be retrieved.
   * @param namespace  the namespace on which this query is resolved.
   * @return the retrieved entity or null if it does not exist.
   * @throws LightChainDistributedStorageException any unhappy path taken on retrieving the Entity.
   */
  @Override
  public Entity get(Identifier identifier, String namespace) throws LightChainDistributedStorageException {
    return this.hub.getEntityFromChannel(identifier, namespace);
  }

  /**
   * Retrieves all entities stored on the underlying DHT of nodes that stored on this channel.
   *
   * @param namespace the namespace on which this query is resolved.
   * @return list of all entities stored on this channel from underlying DHT.
   * @throws LightChainDistributedStorageException any unhappy path taken on retrieving the Entities.
   */
  @Override
  public ArrayList<Entity> allEntities(String namespace) throws LightChainDistributedStorageException {
    return this.hub.getAllEntities(namespace);
  }

}