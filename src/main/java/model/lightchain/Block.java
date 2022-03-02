package model.lightchain;

import model.codec.EntityType;
import model.crypto.Signature;

/**
 * Represents a LightChain Block that encapsulates set of ValidatedTransaction(s).
 */
public class Block extends model.Entity {
  /**
   * Reference to the hash value of another block as its parent.
   */
  private final Identifier previousBlockId;

  /**
   * Identifier of the node that proposes this block (i.e., miner).
   */
  private final Identifier proposer;

  /**
   * Set of validated transactions that this block carries.
   */
  private final ValidatedTransaction[] transactions;

  /**
   * Signature of the proposer over the hash of this block.
   */
  private final Signature signature;

  /**
   * Constructor of the block.
   *
   * @param previousBlockId identifier of a finalized block that this block is extending its snapshot.
   * @param proposer identifier of the node that proposes this block (i.e., miner).
   * @param transactions set of validated transactions that this block carries.
   * @param signature signature of the proposer over the hash of this block.
   */
  public Block(Identifier previousBlockId,
               Identifier proposer,
               ValidatedTransaction[] transactions,
               Signature signature) {
    this.previousBlockId = previousBlockId;
    this.proposer = proposer;
    this.transactions = transactions;
    this.signature = signature;
  }

  /**
   * Type of this entity.
   *
   * @return type of this entity.
   */
  public String type() {
    return EntityType.TYPE_BLOCK;
  }

  public Identifier getPreviousBlockId() {
    return previousBlockId;
  }

  public Identifier getProposer() {
    return proposer;
  }

  public ValidatedTransaction[] getTransactions() {
    return transactions;
  }

  public Signature getSignature() {
    return signature;
  }
}
