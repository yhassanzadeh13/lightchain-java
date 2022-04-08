package network.p2p;

import model.Entity;
import model.exceptions.LightChainDistributedStorageException;
import model.exceptions.LightChainNetworkingException;
import model.lightchain.Identifier;
import protocol.Engine;

import java.io.IOException;

/**
 * Implements Conduit for grpc-based networking layer.
 */
public class P2pConduit implements network.Conduit {
  private P2pNetwork network;
  private Engine engine;

  public P2pConduit(P2pNetwork network, Engine engine) {
    this.network = network;
    this.engine = engine;
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
      network.sendUnicast(e, target, engine);
    } catch (InterruptedException ex) {
      System.out.println("transmission was interrupted during the unicast operation");
      ex.printStackTrace();
      throw new LightChainNetworkingException();
    } catch (IOException ex) { // this will be removed
      System.out.println("transmission was interrupted during the unicast operation");
      ex.printStackTrace();
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
}
