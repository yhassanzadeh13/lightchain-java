package model.lightchain;

import model.crypto.Hash;

public class Transaction extends model.Entity {
  public final Identifier sender;
  public final Identifier receiver;
  public final double amount;

  public Transaction(Identifier sender, Identifier receiver, double amount) {
    this.sender = sender;
    this.receiver = receiver;
    this.amount = amount;
  }

  public Hash Hash(){

    return null;
  }
}
