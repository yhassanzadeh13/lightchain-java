package network;

import java.util.ArrayList;

import model.Entity;
import model.exceptions.LightChainDistributedStorageException;
import model.exceptions.LightChainNetworkingException;
import model.lightchain.Identifier;

/**
 * Conduit represents the Networking interface that is exposed to an Engine.
 */
public interface Conduit {
  /**
   * Sends the Entity through the Network to the remote target.
   *
   * @param e the Entity to be sent over the network.
   * @param target Identifier of the receiver.
   * @throws LightChainNetworkingException any unhappy path taken on sending the Entity.
   */
  void unicast(Entity e, Identifier target) throws LightChainNetworkingException;

  /**
   * Stores given Entity on the underlying Distributed Hash Table (DHT) of nodes.
   *
   * @param e the Entity to be stored over the network.
   * @throws LightChainDistributedStorageException any unhappy path taken on storing the Entity.
   */
  void put(Entity e) throws LightChainDistributedStorageException;

  /**
   * Retrieves the entity corresponding to the given identifier form the underlying Distributed Hash Table
   * (DHT) of nodes.
   *
   * @param identifier identifier of the entity to be retrieved.
   * @return the retrieved entity or null if it does not exist.
   * @throws LightChainDistributedStorageException any unhappy path taken on retrieving the Entity.
   */
  Entity get(Identifier identifier) throws LightChainDistributedStorageException;


  /**
   * Retrieves all entities stored on the underlying DHT of nodes that stored on this channel.
   *
   * @return list of all entities stored on this channel from underlying DHT.
   * @throws LightChainDistributedStorageException any unhappy path taken on retrieving the Entities.
   */
  ArrayList<Entity> allEntities() throws LightChainDistributedStorageException;
}
