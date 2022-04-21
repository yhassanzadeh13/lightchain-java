package model.lightchain;

import java.io.Serializable;

import model.codec.EntityType;
import model.crypto.Signature;

/**
 * A ValidatedTransaction is a wrapper around a Transaction that carries a proof of assigned validators that attests
 * the transaction passed local validation of validators.
 */
public class ValidatedTransaction extends Transaction implements Serializable {
  /**
   * Represents the signatures of assigned validators to this transaction.
   */
  private final Signature[] certificates;

  /**
   * Constructor of the transaction.
   *
   * @param referenceBlockId identifier of a finalized block that this transaction refers to its snapshot.
   * @param sender           identifier of the sender of this transaction.
   * @param receiver         identifier of the receiver of this transaction.
   * @param amount           amount of LightChain tokens that this transaction transfers from sender to receiver.
   * @param certificates     signature of assigned validators to this transaction.
   */
  public ValidatedTransaction(Identifier referenceBlockId,
                              Identifier sender,
                              Identifier receiver,
                              double amount,
                              Signature[] certificates) {

    super(referenceBlockId, sender, receiver, amount);
    this.certificates = certificates.clone();
  }

  public Signature[] getCertificates() {
    return certificates.clone();
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    return super.equals(o);
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
