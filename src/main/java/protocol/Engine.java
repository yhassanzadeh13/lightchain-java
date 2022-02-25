package protocol;

import model.Entity;

/**
 * Engine encapsulates a standalone unit of process in LightChain that is responsible for executing a certain
 * part of the LightChain protocol.
 */
public interface Engine {
  /**
   * Called by Network whenever an Entity is arrived for this engine.
   *
   * @param e the arrived Entity from the network.
   * @throws IllegalArgumentException any unhappy path taken on processing the Entity.
   */
  void process(Entity e) throws IllegalArgumentException;
}
