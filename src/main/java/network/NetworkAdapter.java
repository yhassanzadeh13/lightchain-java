package network;

import model.Entity;
import model.exceptions.LightChainDistributedStorageException;
import model.exceptions.LightChainNetworkingException;
import model.lightchain.Identifier;

import java.util.ArrayList;

/**
 * NetworkAdapter models the interface that is exposed to the conduits from the networking layer.
 */
public interface NetworkAdapter {
  /**
   * Sends the Entity through the Network to the remote target.
   *
   * @param e the Entity to be sent over the network.
   * @param target Identifier of the receiver.
   * @param channel channel on which this entity is sent.
   * @throws LightChainNetworkingException any unhappy path taken on sending the Entity.
   */
  void unicast(Entity e, Identifier target, String channel) throws LightChainNetworkingException;

  /**
   * Stores given Entity on the underlying Distributed Hash Table (DHT) of nodes.
   *
   * @param e the Entity to be stored over the network.
   * @param namespace namespace on which this entity is stored.
   * @throws LightChainDistributedStorageException any unhappy path taken on storing the Entity.
   */
  void put(Entity e, String namespace) throws LightChainDistributedStorageException;

  /**
   * Retrieves the entity corresponding to the given identifier form the underlying Distributed Hash Table
   * (DHT) of nodes.
   *
   * @param identifier identifier of the entity to be retrieved.
   * @param namespace the namespace on which this query is resolved.
   * @return the retrieved entity or null if it does not exist.
   * @throws LightChainDistributedStorageException any unhappy path taken on retrieving the Entity.
   */
  Entity get(Identifier identifier, String namespace) throws LightChainDistributedStorageException;

  /**
   * Retrieves all entities stored on the underlying DHT of nodes that stored on this channel.
   *
   * @param namespace the namespace on which this query is resolved.
   * @return list of all entities stored on this channel from underlying DHT.
   * @throws LightChainDistributedStorageException any unhappy path taken on retrieving the Entities.
   */
  ArrayList<Entity> allEntities(String namespace) throws LightChainDistributedStorageException;
}
