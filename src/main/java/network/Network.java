package network;

import protocol.Engine;

/**
 * Network represents the networking layer of the LightChain node.
 */
public interface Network {
  /**
   * Registers an Engine to the Network by providing it with a Conduit.
   *
   * @param e the Engine to be registered.
   * @param channel the unique channel corresponding to the Engine.
   * @return unique Conduit object created to connect the Network to the Engine.
   * @throws IllegalStateException if the channel is already taken by another Engine.
   */
  Conduit register(Engine e, String channel) throws IllegalStateException;
}
