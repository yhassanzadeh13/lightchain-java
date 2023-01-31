package unittest.fixtures;

import java.util.ArrayList;
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
   * Creates an Account using randomly created PublicKey and given LastBlockId.
   *
   * @param publicKey   unique identifier of the account.
   * @param lastBlockId last block id of the account.
   * @param stake       stake of the account.
   */
  public static Account newAccount(Identifier publicKey, Identifier lastBlockId, int stake) {
    return new Account(publicKey,
        Mockito.mock(
            PublicKey.class,
            Mockito.withSettings().useConstructor(Bytes.byteArrayFixture(32))),
        lastBlockId,
        stake);
  }

  /**
   * Creates staked and unstaked accounts.
   *
   * @param stakedCount   number of staked accounts.
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
   * @param id            identifier of the account to be included.
   * @param stakedCount   number of staked accounts.
   * @param unstakedCount number of unstaked accounts.
   * @return map of staked and unstaked accounts.
   */
  public static HashMap<Identifier, Account> newAccounts(Identifier id, int stakedCount, int unstakedCount) {
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

    accounts.put(id, AccountFixture.newAccount(id, Parameters.MINIMUM_STAKE + 1));
    return accounts;
  }

  /**
   * Creates staked and unstaked accounts including an account with the given identifier and last block id.
   *
   * @param id            identifier of the account to be included.
   * @param stakedCount   number of staked accounts.
   * @param unstakedCount number of unstaked accounts.
   * @return map of staked and unstaked accounts.
   */
  public static ArrayList<Account>[] newAccounts(Identifier id, Identifier lastBlockId1,
                                                 Identifier lastBlockId2, int stakedCount, int unstakedCount) {
    ArrayList<Account> accounts1 = new ArrayList<>();
    ArrayList<Account> accounts2 = new ArrayList<>();
    ArrayList<Account> accounts3 = new ArrayList<>();
    // staked accounts
    for (int i = 0; i < stakedCount; i++) {
      Identifier accountId = IdentifierFixture.newIdentifier();
      Account account = AccountFixture.newAccount(accountId, lastBlockId1, Parameters.MINIMUM_STAKE + i);
      Account account2 = AccountFixture.newAccount(accountId, lastBlockId2, Parameters.MINIMUM_STAKE + i);

      accounts1.add(account);
      accounts2.add(account2);
      accounts3.add(account2);
    }

    // unstaked accounts
    for (int i = 0; i < unstakedCount; i++) {
      Identifier accountId = IdentifierFixture.newIdentifier();
      Account account = AccountFixture.newAccount(accountId, lastBlockId1, Parameters.MINIMUM_STAKE - 2);
      Account account2 = AccountFixture.newAccount(accountId, lastBlockId2, Parameters.MINIMUM_STAKE - 2);

      accounts1.add(account);
      accounts2.add(account2);
      accounts3.add(account2);
    }

    accounts1.add(AccountFixture.newAccount(id, lastBlockId1, Parameters.MINIMUM_STAKE + 1));
    accounts2.add(AccountFixture.newAccount(id, lastBlockId2, Parameters.MINIMUM_STAKE + 1));
    ArrayList<Account>[] accounts = new ArrayList[3];
    accounts[0] = accounts1;
    accounts[1] = accounts2;
    accounts[2] = accounts3;
    return accounts;
  }

  /**
   * Creates staked accounts.
   *
   * @param stakedCount number of staked accounts.
   * @return An array list consist of staked accounts.
   */
  public static ArrayList<Account> newAccounts(int stakedCount) {
    ArrayList<Account> accountArrayList = new ArrayList<>();
    for (int i = 0; i < stakedCount; i++) {
      Identifier accountId = IdentifierFixture.newIdentifier();
      Account account = AccountFixture.newAccount(accountId, Parameters.MINIMUM_STAKE + i);
      accountArrayList.add(account);
    }
    return accountArrayList;
  }
}