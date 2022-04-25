package unittest.fixtures;

import java.util.HashMap;

import model.crypto.PublicKey;
import model.lightchain.Account;
import model.lightchain.Identifier;
import org.mockito.Mockito;
import protocol.Parameters;

/**
 * Encapsulates test utilities for LightChain accounts.
 */
public class AccountFixture {
  /**
   * Constructor of an Account.
   *
   * @param identifier unique identifier of the account.
   *                   Creates an Account using randomly created PublicKey and LastBlockId.
   */
  public static Account newAccount(Identifier identifier) {
    return new Account(
        identifier,
        Mockito.mock(
            PublicKey.class,
            Mockito.withSettings().useConstructor(Bytes.byteArrayFixture(32))),
        IdentifierFixture.newIdentifier(),
        Parameters.MINIMUM_STAKE);
  }

  /**
   * Creates an Account using randomly created PublicKey and LastBlockId.
   *
   * @param identifier unique identifier of the account.
   * @param stake      stake of the account.
   */
  public static Account newAccount(Identifier identifier, int stake) {
    return new Account(identifier,
        Mockito.mock(
            PublicKey.class,
            Mockito.withSettings().useConstructor(Bytes.byteArrayFixture(32))),
        IdentifierFixture.newIdentifier(),
        stake);
  }

  /**
   * Creates staked and unstaked accounts.
   *
   * @param stakedCount number of staked accounts.
   * @param unstakedCount number of unstaked accounts.
   * @return map of staked and unstaked accounts.
   */
  public static HashMap<Identifier, Account> newAccounts(int stakedCount, int unstakedCount) {
    HashMap<Identifier, Account> accounts = new HashMap<>();

    // staked accounts
    for (int i = 0; i < stakedCount; i++) {
      Identifier accountId = IdentifierFixture.newIdentifier();
      Account account = AccountFixture.newAccount(accountId, Parameters.MINIMUM_STAKE + i);
      accounts.put(accountId, account);
    }

    // unstaked accounts
    for (int i = 0; i < unstakedCount; i++) {
      Identifier accountId = IdentifierFixture.newIdentifier();
      Account account = AccountFixture.newAccount(accountId, Parameters.MINIMUM_STAKE - 2);
      accounts.put(accountId, account);
    }

    return accounts;
  }

  /**
   * Creates staked and unstaked accounts including an account with the given identifier.
   *
   * @param id identifier of the account to be included.
   * @param stakedCount number of staked accounts.
   * @param unstakedCount number of unstaked accounts.
   * @return map of staked and unstaked accounts.
   */
  public static HashMap<Identifier, Account> newAccounts(Identifier id,int stakedCount, int unstakedCount) {
    HashMap<Identifier, Account> accounts = new HashMap<>();

    // staked accounts
    for (int i = 0; i < stakedCount; i++) {
      Identifier accountId = IdentifierFixture.newIdentifier();
      Account account = AccountFixture.newAccount(accountId, Parameters.MINIMUM_STAKE + i);
      accounts.put(accountId, account);
    }

    // unstaked accounts
    for (int i = 0; i < unstakedCount; i++) {
      Identifier accountId = IdentifierFixture.newIdentifier();
      Account account = AccountFixture.newAccount(accountId, Parameters.MINIMUM_STAKE - 2);
      accounts.put(accountId, account);
    }

    accounts.put(id, AccountFixture.newAccount(id, Parameters.MINIMUM_STAKE+1));
    return accounts;
  }
}
