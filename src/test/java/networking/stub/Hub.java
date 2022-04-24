package networking.stub;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import model.Entity;
import model.exceptions.LightChainDistributedStorageException;
import model.lightchain.Identifier;
import network.Network;

/**
 * Models the core communication part of the networking layer that allows stub network instances to talk to each other.
 */
public class Hub {
  private final ReentrantReadWriteLock lock;
  private final String channel1 = "test-network-channel-1";
  private final String channel2 = "test-network-channel-2";
  private final ConcurrentHashMap<Identifier, Network> networks;
  private final ConcurrentHashMap<Identifier, Entity> channelMap1;
  private final ConcurrentHashMap<Identifier, Entity> channelMap2;

  /**
   * Create a hub.
   */
  public Hub() {
    this.networks = new ConcurrentHashMap<>();
    this.channelMap1 = new ConcurrentHashMap<>();
    this.channelMap2 = new ConcurrentHashMap<>();
    this.lock = new ReentrantReadWriteLock();
  }

  /**
   * Put Entity to channel.
   *
   * @param e         entitiy.
   * @param namespace channel name.
   * @throws LightChainDistributedStorageException any unhappy path taken on storing the Entity.
   */
  public void putEntityToChannel(Entity e, String namespace) throws LightChainDistributedStorageException {
    try {
      lock.writeLock().lock();
      if (namespace.equals(channel1)) {
        channelMap1.put(e.id(), e);
      } else if (namespace.equals(channel2)) {
        channelMap2.put(e.id(), e);
      } else {
        throw new LightChainDistributedStorageException("entity could not be put the given channel");
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Get entity from channel.
   *
   * @param identifier of entity.
   * @param namespace  channel name.
   * @return entity.
   * @throws LightChainDistributedStorageException any unhappy path taken on storing the Entity.
   */
  public Entity getEntityFromChannel(Identifier identifier, String namespace) throws LightChainDistributedStorageException {
    try {
      lock.readLock().lock();
      if (namespace.equals(channel1)) {
        return channelMap1.get(identifier);
      } else if (namespace.equals(channel2)) {
        return channelMap2.get(identifier);
      } else {
        throw new LightChainDistributedStorageException("could not get the entity");
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Retrieves all entities stored on the underlying DHT of nodes that stored on this channel.
   *
   * @param namespace the namespace on which this query is resolved.
   * @return list of all entities stored on this channel from underlying DHT.
   * @throws LightChainDistributedStorageException any unhappy path taken on retrieving the Entities.
   */
  public ArrayList<Entity> getAllEntities(String namespace) throws LightChainDistributedStorageException {
    if (namespace.equals(channel1)) {
      return new ArrayList<>(channelMap1.values());
    } else if (namespace.equals(channel2)) {
      return new ArrayList<>(channelMap2.values());
    } else {
      throw new LightChainDistributedStorageException("could not get the entities");
    }
  }

  /**
   * Registeration of a network to the Hub.
   *
   * @param identifier identifier of network.
   * @param network    to be registered.
   */
  public void registerNetwork(Identifier identifier, Network network) {
    networks.put(identifier, network);
  }

  /**
   * Transfer entity from to another network on the same channel.
   *
   * @param entity  entity to be transferred.
   * @param target  identifier of target.
   * @param channel channel on which the entity is delivered to target.
   */
  public void transferEntity(Entity entity, Identifier target, String channel) throws IllegalStateException {
    StubNetwork net = this.getNetwork(target);
    try {
      net.receiveUnicast(entity, channel);
    } catch (IllegalArgumentException ex) {
      throw new IllegalStateException("target network failed on receiving unicast: " + ex.getMessage());
    }
  }

  /**
   * Get the network with identifier.
   *
   * @param identifier identity of network.
   * @return network corresponding to identifier.
   */
  private StubNetwork getNetwork(Identifier identifier) {
    return (StubNetwork) networks.get(identifier);
  }
}