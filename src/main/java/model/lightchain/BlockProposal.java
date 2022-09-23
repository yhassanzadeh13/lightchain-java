package model.lightchain;

import java.util.Objects;

import model.Entity;
import model.codec.EntityType;
import model.crypto.Signature;

/**
 * BlockProposal encapsulates an authenticated collection of transactions
 * that is proposed by a LightChain node to be appended to the chain.
 */
public class BlockProposal extends Entity {
  private final BlockHeader header;

  private final BlockPayload payload;

  /**
   * Signature of the proposer over the hash of this block.
   */
  private final Signature proposerSignature;

  /**
   * Constructor of block proposal.
   *
   * @param header            header of this block proposal.
   * @param payload           payload of this block.
   * @param proposerSignature the proposer signature on this block proposal.
   */
  public BlockProposal(BlockHeader header, BlockPayload payload, Signature proposerSignature) {
    this.header = header;
    this.payload = payload;
    this.proposerSignature = proposerSignature;
  }

  @Override
  public String type() {
    return EntityType.TYPE_BLOCK_PROPOSAL;
  }

  public Identifier getPreviousBlockId() {
    return this.header.getPreviousBlockId();
  }

  public Identifier getProposerId() {
    return this.header.getProposerId();
  }

  public ValidatedTransaction[] getTransactions() {
    return this.payload.getTransactions().clone();
  }

  public Signature getSignature() {
    return proposerSignature;
  }

  public long getHeight() {
    return this.header.getHeight();
  }

  public BlockHeader getHeader() {
    return header;
  }

  public BlockPayload getPayload() {
    return payload;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BlockProposal)) return false;
    BlockProposal that = (BlockProposal) o;
    return Objects.equals(getHeader(), that.getHeader())
        && Objects.equals(getPayload(), that.getPayload())
        && Objects.equals(proposerSignature, that.proposerSignature);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getHeader(), getPayload(), proposerSignature);
  }
}
