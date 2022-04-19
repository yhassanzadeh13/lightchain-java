package protocol.engines;

import model.Entity;
import model.lightchain.Identifier;
import protocol.Engine;
import protocol.NewBlockSubscriber;

/**
 * Proposer engine encapsulates the logic of creating new blocks.
 */
public class ProposerEngine implements NewBlockSubscriber, Engine {
  /**
   * OnNewFinalizedBlock notifies the proposer engine of a new validated block. The proposer engine runs validator
   * assigner with the proposer tag and number of validators of 1. If this node is selected, it means that the
   * proposer engine must create a new block.
   * ---
   * Creating new block: proposer engine has a shared storage component with ingest engine, i.e., transactions and
   * blocks. If the minimum number of validated transactions in the pending transactions' storage are available, then
   * proposer engine fetches them, creates a block out of them, runs validator assignment with validation tag, and
   * sends it to the validators.
   *
   * @param blockHeight block height.
   * @param blockId     identifier of block.
   */
  @Override
  public void onNewValidatedBlock(int blockHeight, Identifier blockId) {

  }

  /**
   * The received entity must be only of the BlockApproval type.
   * When a BlockApproval arrives, proposer engine checks if the approval belongs for its recently proposed block, and
   * if it is the case, the approval is stored. When the proposer engine obtains enough approval on its recently
   * proposed block, it creates a validated block out of them and sends it to all nodes (including itself) over the
   * network using the validated blocks channel.
   *
   * @param e the arrived Entity from the network.
   * @throws IllegalArgumentException any entity other than BlockApproval.
   */
  @Override
  public void process(Entity e) throws IllegalArgumentException {

  }
}
