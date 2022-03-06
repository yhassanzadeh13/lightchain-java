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
  private double balance;

  /**
   * The identifier of last finalized block that changed (balance of) this account.
   */
  private final Identifier lastBlockId;

  /**
   * amount of LightChain tokens this account locks in the system in order to be eligible to propose
   * a block or validate transactions and blocks.
   */
  private final int stake;

  /**
   * Constructor of an Account.
   *  @param identifier unique identifier of the account.
   * @param publicKey public key of the account owner.
   * @param lastBlockId identifier of the last block id that changed this account (or genesis id at bootstrap time).
   * @param stake amount of LightChain tokens this account locks in the system in order to be eligible to propose
   */
  public Account(Identifier identifier, PublicKey publicKey, Identifier lastBlockId, int stake) {
    this.identifier = identifier;
    this.publicKey = publicKey;
    this.lastBlockId = lastBlockId;
    this.stake = stake;
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

  public void setBalance(double balance) {
    this.balance = balance;
  }

  public Identifier getLastBlockId() {
    return lastBlockId;
  }
}
