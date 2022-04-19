package model.lightchain;

import model.crypto.Signature;

/**
 * BlockApproval is the data structure containing a signature of a validator over a block.
 */
public class BlockApproval {
  public final Signature signature;
  public final Identifier blockId;

  public BlockApproval(Signature signature, Identifier blockId) {
    this.signature = signature;
    this.blockId = blockId;
  }
}
