package model.lightchain;

import java.util.Arrays;

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
   * @param previousBlockId   identifier of a finalized block that this block is extending its snapshot.
   * @param proposer          identifier of the node that proposes this block (i.e., miner).
   * @param transactions      set of validated transactions that this block carries.
   * @param proposerSignature signature of the proposer over the hash of this block.
   * @param certificates      signature of assigned validators to this transaction.
   */
  public ValidatedBlock(Identifier previousBlockId,
                        Identifier proposer,
                        ValidatedTransaction[] transactions,
                        Signature proposerSignature,
                        Signature[] certificates) {

    super(previousBlockId, proposer, transactions, proposerSignature);
    this.certificates = certificates.clone();
  }

  public Signature[] getCertificates() {
    return certificates.clone();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ValidatedBlock)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    ValidatedBlock that = (ValidatedBlock) o;
    return Arrays.equals(getCertificates(), that.getCertificates());
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + Arrays.hashCode(getCertificates());
    return result;
  }

  @Override
  public String type() {
    return EntityType.TYPE_VALIDATED_TRANSACTION;
  }

  @Override
  public Identifier id() {
    return super.id();
  }
}
