package state.table;

import java.util.ArrayList;

import model.lightchain.Account;
import model.lightchain.Identifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import protocol.Parameters;
import unittest.fixtures.AccountFixture;
import unittest.fixtures.IdentifierFixture;

/**
 * Encapsulates tests for hash table implementation of state snapshot.
 */

public class TableSnapshotTest {
  /**
   * Evaluates the correctness of getters in TableSnapshot.
   */
  @Test
  public void testTableSnapshot() {
    // Arrange
    /// Snapshot and Lists
    Identifier rootBlockId = IdentifierFixture.newIdentifier();
    TableSnapshot tableSnapshot = new TableSnapshot(rootBlockId, 10L);
    ArrayList<Identifier> identifiers = new ArrayList<>();
    ArrayList<Account> accounts = new ArrayList<>();

    /// Staked and Unstaked Accounts
    for (int i = 0; i < 2; i++) {
      // staked
      for (int j = 0; j < 5; j++) {
        Identifier accountId = IdentifierFixture.newIdentifier();
        Account account = AccountFixture.newAccount(accountId, Parameters.MINIMUM_STAKE + j);
        tableSnapshot.addAccount(accountId, account);
        identifiers.add(accountId);
        accounts.add(account);
      }
      for (int k = 0; k < 5; k++) {
        // unstaked
        Identifier accountId = IdentifierFixture.newIdentifier();
        Account account = AccountFixture.newAccount(accountId, Parameters.MINIMUM_STAKE - 2);
        tableSnapshot.addAccount(accountId, account);
        identifiers.add(accountId);
        accounts.add(account);
      }
    }

    // Assert
    for (int i = 0; i < identifiers.size(); i++) {
      Assertions.assertEquals(tableSnapshot.getAccount(identifiers.get(i)), accounts.get(i));
    }
    Assertions.assertEquals(tableSnapshot.getReferenceBlockId(), rootBlockId);
    Assertions.assertEquals(tableSnapshot.getReferenceBlockHeight(), 10L);
    Assertions.assertEquals(tableSnapshot.all().size(), 20);
    Assertions.assertTrue(tableSnapshot.all().containsAll(accounts)
        && accounts.containsAll(tableSnapshot.all()));
  }
}
