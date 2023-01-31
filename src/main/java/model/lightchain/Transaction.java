package model.lightchain;

import java.io.Serializable;

import model.codec.EntityType;
import model.crypto.Signature;

/**
 * Represents a LightChain transaction in form of a token transfer between a sender and receiver.
 */
public class Transaction extends model.Entity implements Serializable {
  /**
   * The identifier of a finalized block that this transaction refers to its snapshot.
   */
  protected final Identifier referenceBlockId;

  /**
   * The identifier of the sender of this transaction.
   */
  protected final Identifier sender;

  /**
   * The identifier of the receiver of this transaction.
   */
  protected final Identifier receiver;

  /**
   * The amount of LightChain tokens that this transaction transfers from sender to receiver.
   */
  protected final double amount;

  /**
   * Valid cryptographic signature of sender that authorizes this transaction.
   */
  protected Signature signature;

  /**
   * Constructor of the transaction.
   *
   * @param referenceBlockId identifier of a finalized block that this transaction refers to its snapshot.
   * @param sender           identifier of the sender of this transaction.
   * @param receiver         identifier of the receiver of this transaction.
   * @param amount           amount of LightChain tokens that this transaction transfers from sender to receiver.
   */
  public Transaction(Identifier referenceBlockId, Identifier sender, Identifier receiver, double amount) {
    this.referenceBlockId = referenceBlockId;
    this.sender = sender;
    this.receiver = receiver;
    this.amount = amount;
  }

  /**
   * Return the HashCode.
   *
   * @return the hashcode.
   */
  @Override
  public int hashCode() {
    return this.id().hashCode();
  }

  /**
   * Returns true if objects are equal.
   *
   * @param o an transaction object.
   * @return true if objects equal.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Transaction)) {
      return false;
    }
    Transaction that = (Transaction) o;

    return this.id().equals(that.id());
  }

  /**
   * Type of this entity.
   *
   * @return type of this entity.
   */
  public String type() {
    return EntityType.TYPE_TRANSACTION;
  }

  public Identifier getReferenceBlockId() {
    return referenceBlockId;
  }

  public Identifier getSender() {
    return sender;
  }

  public Identifier getReceiver() {
    return receiver;
  }

  public double getAmount() {
    return amount;
  }

  public Signature getSignature() {
    return signature;
  }

  public void setSignature(Signature signature) {
    this.signature = signature;
  }
}
