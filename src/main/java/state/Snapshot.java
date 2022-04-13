package state;

import java.util.ArrayList;

import model.lightchain.Account;
import model.lightchain.Identifier;

import java.util.ArrayList;

/**
 * Snapshot represents the status of the protocol state at a given finalized reference block.
 */
public interface Snapshot {
  /**
   * The identifier of finalized block that this snapshot represents.
   *
   * @return the identifier of finalized block that this snapshot represents.
   */
  Identifier getReferenceBlockId();

  /**
   * The height of the reference block that this snapshot represents.
   *
   * @return height of the reference block that this snapshot represents.
   */
  long getReferenceBlockHeight();

  /**
   * Fetches account corresponding to an identifier at the given snapshot.
   *
   * @param identifier identifier of an account of interest.
   * @return account corresponding to the given identifier at this snapshot, or null if such an account
   * does not exist.
   */
  Account getAccount(Identifier identifier);

  /**
   * The list of accounts in this snapshot.
   *
   * @return the list of accounts in this snapshot.
   */
  ArrayList<Account> all();
}
