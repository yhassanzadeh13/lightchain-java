package model.lightchain;

import model.crypto.Signature;

/**
 * TransactionApproval is the data structure containing a signature of a validator over a block.
 */
public class TransactionApproval {
  public final Signature signature;
  public final Identifier transactionId;

  public TransactionApproval(Signature signature, Identifier transactionId) {
    this.signature = signature;
    this.transactionId = transactionId;
  }
}
