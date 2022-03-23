package unittest.fixtures;

import model.crypto.PublicKey;
import model.lightchain.Account;
import model.lightchain.Identifier;
import org.mockito.Mockito;
import protocol.Parameters;

import java.util.HashMap;

public class AccountFixture extends Account {
  /**
   * Constructor of an Account.
   *
   * @param identifier  unique identifier of the account.
   * @param publicKey   public key of the account owner.
   * @param lastBlockId identifier of the last block id that changed this account (or genesis id at bootstrap time).
   */
  public AccountFixture(Identifier identifier, PublicKey publicKey, Identifier lastBlockId, int stake) {
    super(identifier, publicKey, lastBlockId, stake);
  }

  /**
   * Constructor of an Account.
   *
   * @param identifier unique identifier of the account.
   *                   Creates an Account using randomly created PublicKey and LastBlockId.
   */
  public AccountFixture(Identifier identifier) {
    super(identifier, Mockito.mock(
            PublicKey.class,
            Mockito.withSettings().useConstructor(Bytes.byteArrayFixture(32))),
        IdentifierFixture.newIdentifier(),
        Parameters.MINIMUM_STAKE);
  }

  /**
   * Constructor of an Account.
   *
   * @param identifier unique identifier of the account.
   * @param stake      stake of the account.
   *                   <p>
   *                   Creates an Account using randomly created PublicKey and LastBlockId.
   */
  public AccountFixture(Identifier identifier, int stake) {
    super(identifier, Mockito.mock(
            PublicKey.class,
            Mockito.withSettings().useConstructor(Bytes.byteArrayFixture(32))),
        IdentifierFixture.newIdentifier(),
        stake);
  }

  /**
   * Creates 10 staked and 10 unstaked accounts and returns them in a map.
   *
   * @return a map of accounts.
   */
  public static HashMap<Identifier, Account> randomAccounts() {
    HashMap<Identifier, Account> accounts = new HashMap<>();
    /// Staked and Unstaked Accounts
    for (int i = 0; i < 2; i++) {
      for (int j = 0; j < 5; j++) {
        Identifier accountId = IdentifierFixture.newIdentifier();
        Account account = new AccountFixture(accountId, Parameters.MINIMUM_STAKE + j);
        accounts.put(accountId, account);
      }
      for (int k = 0; k < 5; k++) {
        Identifier accountId = IdentifierFixture.newIdentifier();
        Account account = new AccountFixture(accountId, Parameters.MINIMUM_STAKE - 2);
        accounts.put(accountId, account);
      }
    }
    return accounts;
  }
}
