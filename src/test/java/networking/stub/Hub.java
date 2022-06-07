package networking.stub;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import model.Entity;
import model.exceptions.LightChainDistributedStorageException;
import model.lightchain.Identifier;
import network.Network;

/**
 * Models the core communication part of the networking layer that allows stub network instances to talk to each other.
 */
public class Hub {
  private final ConcurrentHashMap<Identifier, Network> networks;

  /**
   * Create a hub.
   */
  public Hub() {
    this.networks = new ConcurrentHashMap<>();
  }

  /**
   * Registeration of a network to the Hub.
   *
   * @param identifier identifier of network.
   * @param network to be registered.
   */
  public void registerNetwork(Identifier identifier, Network network) {
    networks.put(identifier, network);
  }

  /**
   * Transfer entity from to another network on the same channel.
   *
   * @param entity entity to be transferred.
   * @param target identifier of target.
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
public void putEntityToChannel(Entity entity, Identifier target, String channel) throws LightChainDistributedStorageException {
  StubNetwork net = this.getNetwork(target);
  try {
    net.storeEntity(entity,channel);
  } catch (LightChainDistributedStorageException e) {
    throw new LightChainDistributedStorageException("could not store"+e);
  }
}
public Entity getEntityFromChannel(Identifier entityIndentifier, Identifier target, String channel) throws LightChainDistributedStorageException {
    StubNetwork network = this.getNetwork(target);
    return network.provideEntity(entityIndentifier,channel);

}
public ArrayList<Entity> getAllEntities(String namespace) throws LightChainDistributedStorageException {
    ArrayList<Entity> allEntities = new ArrayList<>();
    for (Network network : networks.values()){
      StubNetwork stubNetwork = (StubNetwork) network;
      allEntities.addAll(stubNetwork.allEntities(namespace));
    }
    return allEntities;
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