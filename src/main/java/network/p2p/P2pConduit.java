package network.p2p;

import java.io.IOException;
import java.util.ArrayList;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import model.Entity;
import model.exceptions.LightChainDistributedStorageException;
import model.exceptions.LightChainNetworkingException;
import model.lightchain.Identifier;
import protocol.Engine;

/**
 * Implements Conduit for grpc-based networking layer.
 */
public class P2pConduit implements network.Conduit {
  private final P2pNetwork network;
  private final String channel;

  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "network is intentionally mutable externally")
  public P2pConduit(P2pNetwork network, String channel) {
    this.network = network;
    this.channel = channel;
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
    try {
      network.sendUnicast(e, target, this.channel);
    } catch (IOException | InterruptedException | IllegalArgumentException ex) {
      throw new LightChainNetworkingException("transmission was interrupted during the unicast operation", ex);
    }
  }

  /**
   * Stores given Entity on the underlying Distributed Hash Table (DHT) of nodes.
   *
   * @param e the Entity to be stored over the network.
   * @throws LightChainDistributedStorageException any unhappy path taken on storing the Entity.
   */
  @Override
  public void put(Entity e) throws LightChainDistributedStorageException {

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
    return null;
  }

  @Override
  public ArrayList<Entity> allEntities() throws LightChainDistributedStorageException {
    return null;
  }
}
