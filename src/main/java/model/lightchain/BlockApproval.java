package model.lightchain;

import model.codec.EntityType;
import model.crypto.Signature;

/**
 * BlockApproval is the data structure containing a signature of a validator over a block.
 */
public class BlockApproval extends model.Entity {
  public final Signature signature;
  public final Identifier blockId;

  public BlockApproval(Signature signature, Identifier blockId) {
    this.signature = signature;
    this.blockId = blockId;
  }

  /**
   * Type of this entity.
   *
   * @return type of this entity.
   */
  public String type() {
    return EntityType.TYPE_BLOCK_APPROVAL;
  }

  /**
   * BlockId of the block this approval is for.
   *
   * @return BlockId of the block this approval is for.
   */
  public Identifier getBlockId() {
    return this.blockId;
  }

  /**
   * Signature of this blockApproval.
   *
   * @return signature of this approval
   */
  public Signature getSignature() {
    return this.signature;
  }

}