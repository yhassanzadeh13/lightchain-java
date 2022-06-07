package networking;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import model.Entity;
import model.exceptions.LightChainDistributedStorageException;
import model.exceptions.LightChainNetworkingException;
import model.lightchain.Identifier;
import network.Conduit;
import network.NetworkAdapter;

/**
 * MockConduit represents the Networking interface that is exposed to an Engine.
 */
public class MockConduit implements Conduit {

  private final String channel;
  private final NetworkAdapter networkAdapter;
  private final Set<Identifier> sentEntities;

  /**
   * Constructor.
   *
   * @param channel channel on which it sends the entities.
   * @param adapter instance of the networking layer.
   */
  public MockConduit(String channel, NetworkAdapter adapter) {
    this.channel = channel;
    this.networkAdapter = adapter;
    this.sentEntities = new HashSet<>();
  }

  /**
   * Sends the Entity through the Network to the remote target.
   *
   * @param e      the Entity to be sent over the network.
   * @param target Identifier of the receiver.
   * @throws LightChainNetworkingException any unhappy path taken on sending the Entity.
   */
  @Override
  public void unicast(Entity e, Identifier target) throws LightChainNetworkingException {
    this.sentEntities.add(e.id());
    this.networkAdapter.unicast(e, target, channel);
  }

  /**
   * Stores given Entity on the underlying Distributed Hash Table (DHT) of nodes.
   *
   * @param e the Entity to be stored over the network.
   * @throws LightChainDistributedStorageException any unhappy path taken on storing the Entity.
   */
  @Override
  public void put(Entity e) throws LightChainDistributedStorageException {
    this.networkAdapter.put(e, channel);
  }

  /**
   * Retrieves the entity corresponding to the given identifier form the underlying Distributed Hash Table
   * (DHT) of nodes.
   *
   * @param identifier identifier of the entity to be retrieved.
   * @return the retrieved entity or null if it does not exist.
   * @throws LightChainDistributedStorageException any unhappy path taken on retrieving the Entity.
   */
  @Override
  public Entity get(Identifier identifier) throws LightChainDistributedStorageException {
    return this.networkAdapter.get(identifier, channel);
  }

  @Override
  public ArrayList<Entity> allEntities() throws LightChainDistributedStorageException {
    return this.networkAdapter.allEntities(channel);
  }

  public boolean hasSent(Identifier entityId) {
    return this.sentEntities.contains(entityId);
  }

}