package state.merkle;

import java.util.ArrayList;

import model.codec.EntityType;
import model.lightchain.Account;
import model.lightchain.Identifier;
import modules.ads.MerkleTreeTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import protocol.Parameters;
import unittest.fixtures.AccountFixture;
import unittest.fixtures.IdentifierFixture;

/**
 * Encapsulates tests for Merkle Tree implementation of state snapshot.
 */

public class MerkleSnapshotTest {

  /**
   * Evaluates the correctness of getters in MerkleSnapshot.
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

  /**
   * Tests putting and verifying an account in a merkle tree.
   */
  @Test
  public void testVerification() {
    Account account = AccountFixture.newAccount(IdentifierFixture.newIdentifier());
    MerkleTreeTest.testVerification(account, null);
  }

  /**
   * Tests both putting and getting the same account gives same proof and putting
   * another account gives different proofs.
   */
  @Test
  public void testPutGetSameProof() {
    Account account = AccountFixture.newAccount(IdentifierFixture.newIdentifier());
    MerkleTreeTest.testPutGetSameProof(account, null);
  }

  /**
   * Tests putting an existing account does not change the proof.
   */
  @Test
  public void testPutExistingAccount() {
    Account account = AccountFixture.newAccount(IdentifierFixture.newIdentifier());
    MerkleTreeTest.testPutExistingEntity(account, null);
  }

  /**
   * Concurrently puts and gets accounts and checks their proofs are correct.
   */
  @Test
  public void testConcurrentPutGet() {
    MerkleTreeTest.testConcurrentPutGet(EntityType.TYPE_ACCOUNT, null);
  }

  /**
   * Tests getting an account that does not exist in the merkle tree throws IllegalArgumentException.
   */
  @Test
  public void testGetNonExistingAccount() {
    Account account = AccountFixture.newAccount(IdentifierFixture.newIdentifier());
    MerkleTreeTest.testGetNonExistingEntity(account, null);
  }

  /**
   * Tests inserting null throws IllegalArgumentException.
   */
  @Test
  public void testNullInsertion() {
    MerkleTreeTest.testNullInsertion(null);
  }

  /**
   * Tests the proof verification fails when root is changed.
   */
  @Test
  public void testManipulatedRoot() {
    Account account = AccountFixture.newAccount(IdentifierFixture.newIdentifier());
    MerkleTreeTest.testManipulatedRoot(account, null);
  }

  /**
   * Tests the proof verification fails when an account is changed.
   */
  @Test
  public void testManipulatedAccount() {
    Account account = AccountFixture.newAccount(IdentifierFixture.newIdentifier());
    MerkleTreeTest.testManipulatedEntity(account, null);
  }

  /**
   * Tests the proof fails verification when proof part of authenticated entity of an account is changed.
   */
  @Test
  public void testManipulatedProof() {
    Account account = AccountFixture.newAccount(IdentifierFixture.newIdentifier());
    MerkleTreeTest.testManipulatedProof(account, null);
  }
}
