package network.p2p;

import java.util.HashMap;

import network.Conduit;
import protocol.Engine;

/**
 * Implements a grpc-based networking layer.
 */
public class P2pNetwork implements network.Network {
  private HashMap engineChannelTable;

  public P2pNetwork() {
    this.engineChannelTable = new HashMap<Engine, String>();
  }

  /**
   * Registers an Engine to the Network by providing it with a Conduit.
   *
   * @param e       the Engine to be registered.
   * @param channel the unique channel corresponding to the Engine.
   * @return unique Conduit object created to connect the Network to the Engine.
   * @throws IllegalStateException if the channel is already taken by another Engine.
   */
  @Override
  public Conduit register(Engine e, String channel) throws IllegalStateException {

    if (engineChannelTable.containsKey(channel)) {
      throw new IllegalStateException("channel already exist");
    }

    P2pConduit conduit = new P2pConduit();
    engineChannelTable.put(e, channel);
    return conduit;

  }
}
