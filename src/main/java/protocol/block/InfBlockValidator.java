package protocol.block;

import model.lightchain.Block;
import model.lightchain.BlockProposal;

/**
 * Validator encapsulates the logic of a block validator in LightChain.
 */
public interface InfBlockValidator {
  /**
   * Validates block proposal parameters.
   *
   * @param proposal the block under validation.
   * @return true if all proposal fields have a valid value, and false otherwise. A proposal is valid if the
   * previous block id is a valid and finalized block, the proposer refers to a valid identity at
   * snapshot of the previous block id, and the number of transactions are within the permissible range of LightChain
   * parameters.
   */
  boolean isCorrect(BlockProposal proposal);

  /**
   * Validates the consistency of block based on LightChain protocol.
   *
   * @param block the block under validation.
   * @return true only if the previous block id this block refers to corresponds to the last snapshot on the
   * node's state. False otherwise.
   */
  boolean isConsistent(Block block);

  /**
   * Validates that the block is indeed issued by the proposer.
   *
   * @param block the block under validation.
   * @return true if the block has a valid signature that is verifiable by the public key of its proposer,
   * false otherwise.
   */
  boolean isAuthenticated(Block block);

  /**
   * Validates proposer has enough stake.
   *
   * @param block the block under validation.
   * @return true if proposer has a greater than or equal stake than the amount of the minimum required one based on
   * LightChain parameters, and false otherwise.
   * The stake of proposer must be checked at the snapshot of the reference block of the block.
   */
  boolean proposerHasEnoughStake(Block block);

  /**
   * Checks all transactions included in the block are validated.
   *
   * @param block the block under validation.
   * @return true if all transactions included in the block are validated, i.e., have a minimum of signature threshold
   * as specified by LightChain protocol. Signatures are verified based on the public key of validators at the snapshot
   * of the previous block id.
   * Also, all validators of each transaction has minimum stake at the previous block id of this block.
   */
  boolean allTransactionsValidated(Block block);

  /**
   * Checks all transactions included in the block are sound.
   *
   * @param block the block under validation.
   * @return true if all transactions included in the block are sound. Each individual transaction is sound
   * if its reference block id has a strictly higher height than the height of the
   * last block id in the sender account.
   */
  boolean allTransactionsSound(Block block);

  /**
   * Checks no two validated transactions included in this block have the same sender.
   *
   * @param block the block under validation.
   * @return true if no two validated transactions included in this block have the same sender. False otherwise.
   */
  boolean noDuplicateSender(Block block);
}
