package model.lightchain;

import model.codec.EntityType;
import model.crypto.Signature;

/**
 * A ValidatedBlock is a wrapper around a Block that carries a proof of assigned validators that attests
 * the block passed local validation of validators.
 */
public class ValidatedBlock extends Block {
  /**
   * Represents the signatures of assigned validators to this block.
   */
  private final Signature[] certificates;

  /**
   * Constructor of the validated block.
   *
   * @param previousBlockId identifier of a finalized block that this block is extending its snapshot.
   * @param proposer        identifier of the node that proposes this block (i.e., miner).
   * @param transactions    set of validated transactions that this block carries.
   * @param signature       signature of the proposer over the hash of this block.
   * @param certificates    signature of assigned validators to this transaction.
   * @param height          height of the block.
   */
  public ValidatedBlock(Identifier previousBlockId,
                        Identifier proposer,
                        ValidatedTransaction[] transactions,
                        Signature signature,
                        Signature[] certificates,
                        int height) {
    super(previousBlockId, proposer, height, transactions, signature);
    this.certificates = certificates.clone();
  }

  public Signature[] getCertificates() {
    return certificates.clone();
  }

  @Override
  public String type() {
    return EntityType.TYPE_VALIDATED_BLOCK;
  }

  @Override
  public Identifier id() {
    return super.id();
  }
}