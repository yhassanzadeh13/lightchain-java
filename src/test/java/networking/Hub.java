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
   * @param key identifier of network.
   * @param network to be registered.
   */
  public void registerNetwork(Identifier key, Network network) {
    networks.put(key, network);

  }

  /**
   * Transfer entity from a stubnetwork to another through hub.
   *
   * @param entity entity to be transferred.
   * @param identifier identifier of target.
   * @param channel channel of the transmitter-target engine.
   */
  public void transferEntity(Entity entity, Identifier identifier, String channel) {
    StubNetwork net = this.getNetwork(identifier);
    net.receiveUnicast(entity, channel);

  }

  /**
   * Get the network with identifier.
   *
   * @param key identity of.
   * @return network.
   */
  private StubNetwork getNetwork(Identifier key) {
    return (StubNetwork) networks.get(key);
  }

}