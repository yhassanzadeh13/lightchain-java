package model.lightchain;

import java.io.Serializable;

import model.codec.EntityType;
import model.crypto.Signature;

/**
 * Represents a LightChain Block that encapsulates set of ValidatedTransaction(s).
 */
public class Block extends model.Entity implements Serializable {
  private final BlockProposal proposal;

  /**
   * Represents the signatures of assigned validators on the proposal of this block.
   */
  private final Signature[] certificates;

  /**
   * Constructor of the block.
   *
   * @param proposal     the block proposal.
   * @param certificates signature of validators on this block.
   */
  public Block(BlockProposal proposal, Signature[] certificates) {
    this.proposal = proposal;
    this.certificates = certificates;
  }

  public BlockProposal getProposal() {
    return proposal;
  }

  public Signature[] getCertificates() {
    return certificates;
  }

  @Override
  public int hashCode() {
    return this.id().hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Block)) {
      return false;
    }
    Block that = (Block) o;

    return this.id().equals(that.id());
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
    return this.proposal.getPreviousBlockId();
  }

  public Identifier getProposerId() {
    return this.proposal.getProposerId();
  }

  public ValidatedTransaction[] getTransactions() {
    return this.getProposal().getTransactions().clone();
  }

  public Signature getProposerSignature() {
    return this.getProposal().getSignature();
  }

  public long getHeight() {
    return this.getProposal().getHeight();
  }
}