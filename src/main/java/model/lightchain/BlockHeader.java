package model.lightchain;

import model.Entity;
import model.codec.EntityType;

public class BlockHeader extends Entity {
  /**
   * Reference to the hash value of another block as its parent.
   */
  private final Identifier previousBlockId;

  /**
   * Height of the block.
   * Height of this block header (height of genesis is 0, and height of a block is 1 + height of previous block).
   */
  private final long height;

  /**
   * Identifier of the node that proposes this block (i.e., miner).
   */
  private final Identifier proposerId;

  /**
   * Identifier of the payload of this header.
   */
  private final Identifier payloadId;


  /**
   * Constructor of the block header.
   * @param height height of this block header (height of genesis is 0, and height of a block is 1 + height of previous block).
   * @param previousBlockId identifier of a finalized block that this block is extending its snapshot.
   * @param proposer        identifier of the node that proposes this block (i.e., miner).
   * @param payloadId       identifier of the payload of this block.
   */
  public BlockHeader(long height,
                     Identifier previousBlockId,
                     Identifier proposer,
                     Identifier payloadId) {
    this.previousBlockId = previousBlockId;
    this.proposerId = proposer;
    this.payloadId = payloadId;
    this.height = height;
  }

  public Identifier getPreviousBlockId() {
    return previousBlockId;
  }

  public long getHeight() {
    return height;
  }

  public Identifier getProposerId() {
    return proposerId;
  }

  public Identifier getPayloadId() {
    return payloadId;
  }

  @Override
  public String type() {
    return EntityType.TYPE_BLOCK_HEADER;
  }
}
