package model.lightchain;

import model.crypto.PublicKey;

/**
 * Represents a LightChain account which is the constituent of the SnapShot.
 */
public class Account {
  /**
   * Unique identifier of the account.
   */
  private final Identifier identifier;

  /**
   * Public key corresponding to this account.
   */
  private final PublicKey publicKey;

  /**
   * Account balance in LightChain tokens.
   */
  private final double balance;

  /**
   * The identifier of last finalized block that changed (balance of) this account.
   */
  private final Identifier lastBlockId;

  /**
   * Constructor of an Account.
   *
   * @param identifier unique identifier of the account.
   * @param publicKey public key of the account owner.
   * @param lastBlockId identifier of the last block id that changed this account (or genesis id at bootstrap time).
   */
  public Account(Identifier identifier, PublicKey publicKey, Identifier lastBlockId) {
    this.identifier = identifier;
    this.publicKey = publicKey;
    this.lastBlockId = lastBlockId;
    this.balance = 0;
  }

  public Identifier getIdentifier() {
    return identifier;
  }

  public PublicKey getPublicKey() {
    return publicKey;
  }

  public double getBalance() {
    return balance;
  }

  public Identifier getLastBlockId() {
    return lastBlockId;
  }
}
