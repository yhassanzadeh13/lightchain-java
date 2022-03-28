package protocol.assigner;

import java.util.ArrayList;

import model.lightchain.Account;
import model.lightchain.Assignment;
import model.lightchain.Identifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import state.Snapshot;
import unittest.fixtures.AccountFixture;
import unittest.fixtures.IdentifierFixture;

import static org.mockito.Mockito.*;

/**
 * Encapsulates tests for validator assignment.
 */
public class AssignerTest {

  /**
   * All tests consider a snapshot of 10 staked accounts and 10 unstaked accounts as well as a mock identifier
   */

  /**
   * Tests the assigner to choose one of the staked accounts and confirms it always picks the same account by
   * running the test 100 times.
   */
  @Test
  public void testAssignerPicksOneStakedAccount() {
    // Arrange
    Identifier entityId = IdentifierFixture.newIdentifier();
    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.randomAccounts().values());
    Snapshot snapshot = mock(Snapshot.class);
    when(snapshot.all()).thenReturn(accounts);

    // Act
    LightChainValidatorAssigner assigner = new LightChainValidatorAssigner();
    Assignment assignment = assigner.assign(entityId, snapshot, (short) 1);

    boolean sameAccount = true;
    for (int i = 0; i < 9; i++) {
      Assignment assignment2 = assigner.assign(entityId, snapshot, (short) 1);

      if (!assignment.equals(assignment2)) {
        Assertions.fail();
      }
    }

    // Assert
    Assertions.assertTrue(sameAccount);
  }

  /**
   * Tests the assigner to choose two of the staked accounts and confirms it always picks the same five accounts by
   * running the test 100 times.
   */
  @Test
  public void testAssignerPicksTwoStakedAccount() {
    // Arrange
    Identifier entityId = IdentifierFixture.newIdentifier();
    Account a = AccountFixture.newAccount(entityId);
    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.randomAccounts().values());
    Snapshot snapshot = mock(Snapshot.class);
    when(snapshot.all()).thenReturn(accounts);

    // Act
    LightChainValidatorAssigner assigner = new LightChainValidatorAssigner();
    Assignment assignment = assigner.assign(entityId, snapshot, (short) 2);

    boolean sameAccount = true;
    for (int i = 0; i < 99; i++) {
      Assignment assignment2 = assigner.assign(entityId, snapshot, (short) 2);
      if (!assignment.equals(assignment2)) {
        Assertions.fail();
      }
    }

    // Assert
    Assertions.assertTrue(sameAccount);
  }

  /**
   * Tests the assigner to choose five of the staked accounts and confirms it always picks the same five accounts by
   * running the test 100 times.
   */
  @Test
  public void testAssignerPicksFiveStakedAccount() {
    // Arrange
    Identifier entityId = IdentifierFixture.newIdentifier();
    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.randomAccounts().values());
    Snapshot snapshot = mock(Snapshot.class);
    when(snapshot.all()).thenReturn(accounts);

    // Act
    LightChainValidatorAssigner assigner = new LightChainValidatorAssigner();
    Assignment assignment = assigner.assign(entityId, snapshot, (short) 5);

    boolean sameAccount = true;
    for (int i = 0; i < 99; i++) {
      Assignment assignment2 = assigner.assign(entityId, snapshot, (short) 5);
      if (!assignment.equals(assignment2)) {
        Assertions.fail();
      }
    }

    // Assert
    Assertions.assertTrue(sameAccount);
  }

  /**
   * Tests the assigner to choose ten of the staked accounts and confirms it always picks the same ten accounts by
   * running the test 100 times.
   */
  @Test
  public void testAssignerPicksTenStakedAccount() {
    // Arrange
    Identifier entityId = IdentifierFixture.newIdentifier();
    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.randomAccounts().values());
    Snapshot snapshot = mock(Snapshot.class);
    when(snapshot.all()).thenReturn(accounts);

    // Act
    LightChainValidatorAssigner assigner = new LightChainValidatorAssigner();
    Assignment assignment = assigner.assign(entityId, snapshot, (short) 10);

    boolean sameAccount = true;
    for (int i = 0; i < 99; i++) {
      Assignment assignment2 = assigner.assign(entityId, snapshot, (short) 10);
      if (!assignment.equals(assignment2)) {
        Assertions.fail();
      }
    }

    // Assert
    Assertions.assertTrue(sameAccount);
  }

  /**
   * Tests the assigner to choose eleven of the staked accounts and confirms it fails since there is not enough accounts
   * and returns IllegalArgumentException.
   *
   * @throws IllegalArgumentException
   */
  @Test
  public void testAssignerFails_NotEnoughAccount() throws IllegalArgumentException {
    // Arrange
    Identifier entityId = IdentifierFixture.newIdentifier();
    Snapshot snapshot = mock(Snapshot.class);
    when(snapshot.all()).thenReturn(new ArrayList<>(AccountFixture.randomAccounts().values()));

    // Act
    LightChainValidatorAssigner assigner = new LightChainValidatorAssigner();
    try {
      Assignment assignment = assigner.assign(entityId, snapshot, (short) 11);
      Assertions.fail();
    } catch (IllegalArgumentException e) {
      Assertions.assertEquals("not enough accounts in the snapshot", e.getMessage());
    }
  }

  /**
   * Tests the assigner to choose 1 account when the snapshot is empty and confirms it fails and returns IllegalArgumentException.
   *
   * @throws IllegalArgumentException
   */
  @Test
  public void testAssignerFails_NullAccountList() throws IllegalArgumentException {
    // Arrange
    Identifier entityId = IdentifierFixture.newIdentifier();
    Snapshot snapshot = mock(Snapshot.class);
    when(snapshot.all()).thenReturn(new ArrayList<>());

    // Act
    LightChainValidatorAssigner assigner = new LightChainValidatorAssigner();
    try {
      Assignment assignment = assigner.assign(entityId, snapshot, (short) 1);
      Assertions.fail();
    } catch (IllegalArgumentException e) {
      Assertions.assertEquals("not enough accounts in the snapshot", e.getMessage());
    }
  }

  /**
   * Tests the assigner confirms it fails and returns IllegalArgumentException when given identifier is null.
   *
   * @throws IllegalArgumentException
   */
  @Test
  public void testAssignerFails_NullIdentifier() throws IllegalArgumentException {
    // Arrange
    Snapshot snapshot = mock(Snapshot.class);
    when(snapshot.all()).thenReturn(new ArrayList<>());

    // Act
    LightChainValidatorAssigner assigner = new LightChainValidatorAssigner();
    try {
      Assignment assignment = assigner.assign(null, snapshot, (short) 1);
      Assertions.fail();
    } catch (IllegalArgumentException e) {
      Assertions.assertEquals("identifier cannot be null", e.getMessage());
    }

  }

  /**
   * Tests the assigner confirms it fails and returns IllegalArgumentException when given snapshot is null.
   *
   * @throws IllegalArgumentException
   */
  @Test
  public void testAssignerFails_NullSnapshot() throws IllegalArgumentException {
    // Arrange
    Identifier entityId = IdentifierFixture.newIdentifier();

    // Act
    LightChainValidatorAssigner assigner = new LightChainValidatorAssigner();
    try {
      Assignment assignment = assigner.assign(entityId, null, (short) 1);
      Assertions.fail();
    } catch (IllegalArgumentException e) {
      Assertions.assertEquals("snapshot cannot be null", e.getMessage());
    }

  }

  /**
   * Tests the assigner to choose 0 of the staked accounts and confirms it returns an empty assignment.
   */
  @Test
  public void testAssignerPicksZeroStakedAccount() {
    // Arrange
    Identifier entityId = IdentifierFixture.newIdentifier();
    Snapshot snapshot = mock(Snapshot.class);
    when(snapshot.all()).thenReturn(new ArrayList<>(AccountFixture.randomAccounts().values()));

    // Act
    LightChainValidatorAssigner assigner = new LightChainValidatorAssigner();
    Assignment assignment = assigner.assign(entityId, snapshot, (short) 0);

    // Assert
    Assertions.assertEquals(assignment, new Assignment());
  }
}
