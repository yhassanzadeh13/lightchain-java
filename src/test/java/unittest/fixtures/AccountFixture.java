package unittest.fixtures;

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
            Mockito.withSettings().useConstructor((Object) Bytes.byteArrayFixture(32))),
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
            Mockito.withSettings().useConstructor((Object) Bytes.byteArrayFixture(32))),
        IdentifierFixture.newIdentifier(),
        stake);
  }
}
