package model.lightchain;

import model.crypto.Hash;
import model.crypto.PublicKey;

public class Account {
  private final Identifier identifier;
  private final PublicKey publicKey;
  private double balance;
  private Hash lastBlockId;

  public Account(Identifier identifier, PublicKey publicKey) {
    this.identifier = identifier;
    this.publicKey = publicKey;
    this.balance = 0;
  }
}
