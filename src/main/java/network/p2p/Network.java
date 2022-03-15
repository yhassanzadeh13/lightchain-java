package network.p2p;

import network.Conduit;
import protocol.Engine;

/**
 * Implements a grpc-based networking layer.
 */
public class Network implements network.Network {
  /**
   * Registers an Engine to the Network by providing it with a Conduit.
   *
   * @param e the Engine to be registered.
   * @param channel the unique channel corresponding to the Engine.
   * @return unique Conduit object created to connect the Network to the Engine.
   * @throws IllegalStateException if the channel is already taken by another Engine.
   */
  @Override
  public Conduit register(Engine e, String channel) throws IllegalStateException {
    return null;
  }
}
