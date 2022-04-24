package state.merkle;

import java.util.ArrayList;

import model.lightchain.Account;
import model.lightchain.Identifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import protocol.Parameters;
import state.table.TableSnapshot;
import unittest.fixtures.AccountFixture;
import unittest.fixtures.IdentifierFixture;

/**
 * Encapsulates tests for hash table implementation of state snapshot.
 */

public class MerkleSnapshotTest {
  /**
   * Evaluates the correctness of getters in TableSnapshot.
   */
  @Test
  public void testMerkleSnapshot() {
    // Arrange
    /// Snapshot and Lists
    Identifier rootBlockId = IdentifierFixture.newIdentifier();
    MerkleSnapshot merkleSnapshot = new MerkleSnapshot(rootBlockId, 10L);
    ArrayList<Identifier> identifiers = new ArrayList<>();
    ArrayList<Account> accounts = new ArrayList<>();

    /// Staked and Unstaked Accounts
    for (int i = 0; i < 2; i++) {
      // staked
      for (int j = 0; j < 5; j++) {
        Identifier accountId = IdentifierFixture.newIdentifier();
        Account account = AccountFixture.newAccount(accountId, Parameters.MINIMUM_STAKE + j);
        merkleSnapshot.addAccount(accountId, account);
        identifiers.add(accountId);
        accounts.add(account);
      }
      for (int k = 0; k < 5; k++) {
        // unstaked
        Identifier accountId = IdentifierFixture.newIdentifier();
        Account account = AccountFixture.newAccount(accountId, Parameters.MINIMUM_STAKE - 2);
        merkleSnapshot.addAccount(accountId, account);
        identifiers.add(accountId);
        accounts.add(account);
      }
    }

    // Assert
    for (int i = 0; i < identifiers.size(); i++) {
      Assertions.assertEquals(merkleSnapshot.getAccount(identifiers.get(i)), accounts.get(i));
    }
    Assertions.assertEquals(merkleSnapshot.getReferenceBlockId(), rootBlockId);
    Assertions.assertEquals(merkleSnapshot.getReferenceBlockHeight(), 10L);
    Assertions.assertEquals(merkleSnapshot.all().size(), 20);
    Assertions.assertTrue(merkleSnapshot.all().containsAll(accounts)
        && accounts.containsAll(merkleSnapshot.all()));
  }
}
