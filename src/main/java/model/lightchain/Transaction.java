package model.lightchain;

import model.codec.EntityType;
import model.crypto.Signature;

/**
 * Represents a LightChain transaction in form of a token transfer between a sender and receiver.
 */
public class Transaction extends model.Entity {
  /**
   * The identifier of a finalized block that this transaction refers to its snapshot.
   */
  private final Identifier referenceBlockId;

  /**
   * The identifier of the sender of this transaction.
   */
  private final Identifier sender;

  /**
   * The identifier of the receiver of this transaction.
   */
  private final Identifier receiver;

  /**
   * The amount of LightChain tokens that this transaction transfers from sender to receiver.
   */
  private final double amount;

  /**
   * Valid cryptographic signature of sender that authorizes this transaction.
   */
  private Signature signature;

  public void setSignature(Signature signature) {
    this.signature = signature;
  }

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
}
