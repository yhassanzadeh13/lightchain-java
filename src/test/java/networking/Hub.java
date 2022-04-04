package networking;

import java.util.concurrent.ConcurrentHashMap;

import model.Entity;
import model.lightchain.Identifier;
import network.Network;

/**
 * Models the core communication part of the networking layer that allows stub network instances to talk to each other.
 */
public class Hub {
  private final ConcurrentHashMap<Identifier, Network> networks;
  private final ConcurrentHashMap<Identifier, Entity> entities;

  /**
   * Create a hub.
   */
  public Hub() {
    this.networks = new ConcurrentHashMap<>();
    this.entities = new ConcurrentHashMap<>();
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
  public void transferEntity(Entity entity, Identifier target, String channel) {
    StubNetwork net = this.getNetwork(target);
    net.receiveUnicast(entity, channel);

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