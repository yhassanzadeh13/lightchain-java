package networking.stub;

import java.util.*;
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
  private final List<Identifier> identifierSet;
  private final ConcurrentHashMap<Identifier, Network> networks;
  private final ConcurrentHashMap<Identifier, ConcurrentHashMap<Identifier, Entity>> channelMap1;
  private final ConcurrentHashMap<Identifier, ConcurrentHashMap<Identifier, Entity>> channelMap2;

  /**
   * Create a hub.
   */
  public Hub() {
    this.lock = new ReentrantReadWriteLock();
    this.networks = new ConcurrentHashMap<>();
    this.channelMap1 = new ConcurrentHashMap<>();
    this.channelMap2 = new ConcurrentHashMap<>();
    this.identifierSet = new ArrayList<>();
  }

  /**
   * Registeration of a network to the Hub.
   *
   * @param identifier identifier of network.
   * @param network    to be registered.
   */
  public void registerNetwork(Identifier identifier, Network network) {
    networks.putIfAbsent(identifier, network);
    channelMap1.putIfAbsent(identifier, new ConcurrentHashMap<>());
    channelMap2.putIfAbsent(identifier, new ConcurrentHashMap<>());
    identifierSet.add(identifier);
    sortMaps();

  }

  /**
   * Sort the identifier of networks.
   */
  public void sortMaps() {
    Collections.sort(identifierSet);
  }

  /**
   * Put Entity to Node.
   *
   * @param e         entitiy.
   * @param namespace channel name.
   */
  public void putEntityToChannel(Entity e, String namespace) throws LightChainDistributedStorageException {
    try {
      lock.writeLock().lock();
      Identifier identifierOfNetwork = binarySearch(e.id());
      if (namespace.equals(channel1)) {
        channelMap1.get(identifierOfNetwork).put(e.id(), e);
      } else if (namespace.equals(channel2)) {
        channelMap2.get(identifierOfNetwork).put(e.id(), e);
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
   */
  public Entity getEntityFromChannel(Identifier identifier, String namespace) throws LightChainDistributedStorageException {
    try {
      lock.readLock().lock();
      Identifier identifierOfNetwork = binarySearch(identifier);
      if (namespace.equals(channel1)) {
        return channelMap1.get(identifierOfNetwork).get(identifier);
      } else if (namespace.equals(channel2)) {
        return channelMap2.get(identifierOfNetwork).get(identifier);
      } else {
        throw new LightChainDistributedStorageException("could not get the entity");
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Get the identifier of the network that store the entity.
   *
   * @param identifier identifier of the entity to be stored.
   * @return identifier of the network that store the entity.
   */
  public Identifier binarySearch(Identifier identifier) {
    int left = 0, right = identifierSet.size() - 1;
    while (left < right) {
      int mid = left + (right - left) / 2;
      if (identifierSet.get(mid).compareTo(identifier) == 0) {
        return identifierSet.get(mid);
      }
      if (identifier.compareTo(identifierSet.get(mid)) > 0) {
        left = mid + 1;
      } else {
        right = mid - 1;
      }
    }
    return identifierSet.get(left);
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

  /**
   * Retrieves all entities stored on the underlying DHT of nodes that stored on this channel.
   *
   * @param namespace the namespace on which this query is resolved.
   * @return list of all entities stored on this channel from underlying DHT.
   * @throws LightChainDistributedStorageException any unhappy path taken on retrieving the Entities.
   */
  public ArrayList<Entity> getAllEntities(String namespace) throws LightChainDistributedStorageException {
    ArrayList<Entity> allEntities = new ArrayList<>();
    if (namespace.equals(channel1)) {
      for (Identifier identifier : channelMap1.keySet()) {
        List<Entity> entityList = new ArrayList<>(channelMap1.get(identifier).values());
        allEntities.addAll(entityList);
      }
      return allEntities;
    } else if (namespace.equals(channel2)) {
      for (Identifier identifier : channelMap2.keySet()) {
        List<Entity> entityList = new ArrayList<>(channelMap2.get(identifier).values());
        allEntities.addAll(entityList);
      }
      return allEntities;
    } else {
      throw new LightChainDistributedStorageException("could not get the entities");
    }
  }
}