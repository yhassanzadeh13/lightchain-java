package protocol.engines;

import model.Entity;
import protocol.Engine;

/**
 * ValidatorEngine is a standalone engine of LightChain that runs transaction and block validation.
 */
public class ValidatorEngine implements Engine {


  /**
   * Received entity to this engine can be either a block or a transaction, anything else should throw an exception.
   * Upon receiving a block or transaction, the engine runs the assignment and checks whether the current node
   * is an assigned validator for that transaction or block. If the current node is not assigned, the engine
   * discards the entity.
   * If received entity is a block, the engine runs block validation on it, and if it passes the validation,
   * the engine signs the identifier of that block and sends the signature to the proposer of the block.
   * If received entity is a transaction, it runs the transaction validation on it.
   * If the transaction passes validation,
   * the engine signs the identifier of that transaction and sends the signature to the sender of that transaction.
   *
   * @param e the arrived Entity from the network, it should be either a transaction or a block.
   * @throws IllegalArgumentException when the arrived entity is neither a transaction nor a block.
   */
  @Override
  public void process(Entity e) throws IllegalArgumentException {
  }
}