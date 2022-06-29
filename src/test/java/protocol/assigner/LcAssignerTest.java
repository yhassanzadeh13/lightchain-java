package protocol.assigner;

import java.util.ArrayList;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import model.lightchain.Account;
import model.lightchain.Assignment;
import model.lightchain.Identifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import state.Snapshot;
import unittest.fixtures.AccountFixture;
import unittest.fixtures.IdentifierFixture;

/**
 * Encapsulates tests for validator assignment.
 * All tests consider a snapshot of 10 staked accounts and 10 unstaked accounts as well as a mock identifier
 */
public class AssignerTest {
  /**
   * Tests the assigner to choose one of the staked accounts and confirms it always picks the same account by
   * running the test 100 times.
   */
  @Test
  public void testAssignerPicksOneStakedAccount() {
    // Arrange
    Identifier entityId = IdentifierFixture.newIdentifier();
    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    Snapshot snapshot = mock(Snapshot.class);
    when(snapshot.all()).thenReturn(accounts);

    // Act
    LightChainAssigner assigner = new LightChainAssigner();
    Assignment assignment = assigner.assign(entityId, snapshot, (short) 1);
    Assertions.assertEquals(1, assignment.size());

    boolean sameAccount = true;
    for (int i = 0; i < 100; i++) {
      Assignment assignment2 = assigner.assign(entityId, snapshot, (short) 1);
      Assertions.assertEquals(1, assignment2.size());
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
    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    Snapshot snapshot = mock(Snapshot.class);
    when(snapshot.all()).thenReturn(accounts);

    // Act
    LightChainAssigner assigner = new LightChainAssigner();
    Assignment assignment = assigner.assign(entityId, snapshot, (short) 2);
    Assertions.assertEquals(2, assignment.size());

    boolean sameAccount = true;
    for (int i = 0; i < 100; i++) {
      Assignment assignment2 = assigner.assign(entityId, snapshot, (short) 2);
      Assertions.assertEquals(2, assignment2.size());

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
    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    Snapshot snapshot = mock(Snapshot.class);
    when(snapshot.all()).thenReturn(accounts);

    // Act
    LightChainAssigner assigner = new LightChainAssigner();
    Assignment assignment = assigner.assign(entityId, snapshot, (short) 5);
    Assertions.assertEquals(5, assignment.size());

    boolean sameAccount = true;
    for (int i = 0; i < 100; i++) {
      Assignment assignment2 = assigner.assign(entityId, snapshot, (short) 5);
      Assertions.assertEquals(5, assignment2.size());

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
    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    Snapshot snapshot = mock(Snapshot.class);
    when(snapshot.all()).thenReturn(accounts);

    // Act
    LightChainAssigner assigner = new LightChainAssigner();
    Assignment assignment = assigner.assign(entityId, snapshot, (short) 10);
    Assertions.assertEquals(10, assignment.size());

    boolean sameAccount = true;
    for (int i = 0; i < 100; i++) {
      Assignment assignment2 = assigner.assign(entityId, snapshot, (short) 10);
      Assertions.assertEquals(10, assignment2.size());
      if (!assignment.equals(assignment2)) {
        Assertions.fail();
      }
    }

    // Assert
    Assertions.assertTrue(sameAccount);
  }

  /**
   * Tests the assigner to choose 11 of the staked accounts out of 10 available
   * and confirms it fails since there is not enough accounts and returns IllegalArgumentException.
   */
  @Test
  public void testAssignerFails_NotEnoughAccount() throws IllegalArgumentException {
    // Arrange
    Identifier entityId = IdentifierFixture.newIdentifier();
    Snapshot snapshot = mock(Snapshot.class);
    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    when(snapshot.all()).thenReturn(accounts);

    // Act
    LightChainAssigner assigner = new LightChainAssigner();
    try {
      assigner.assign(entityId, snapshot, (short) 11); // picking more validators than exists on snapshot.
      Assertions.fail();
    } catch (IllegalArgumentException e) {
      Assertions.assertEquals(LightChainAssigner.NOT_ENOUGH_ACCOUNTS, e.getMessage());
    }
  }

  /**
   * Tests the assigner to choose 1 account when the snapshot is empty and confirms it fails
   * and returns IllegalArgumentException.
   */
  @Test
  public void testAssignerFails_NullAccountList() throws IllegalArgumentException {
    // Arrange
    Identifier entityId = IdentifierFixture.newIdentifier();
    Snapshot snapshot = mock(Snapshot.class);
    when(snapshot.all()).thenReturn(new ArrayList<>());

    // Act
    LightChainAssigner assigner = new LightChainAssigner();
    try {
      assigner.assign(entityId, snapshot, (short) 1);
      Assertions.fail();
    } catch (IllegalArgumentException e) {
      Assertions.assertEquals(LightChainAssigner.NOT_ENOUGH_ACCOUNTS, e.getMessage());
    }
  }

  /**
   * Tests the assigner confirms it fails and returns IllegalArgumentException when given identifier is null.
   */
  @Test
  public void testAssignerFails_NullIdentifier() throws IllegalArgumentException {
    // Arrange
    Snapshot snapshot = mock(Snapshot.class);
    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    when(snapshot.all()).thenReturn(accounts);

    // Act
    LightChainAssigner assigner = new LightChainAssigner();
    try {
      assigner.assign(null, snapshot, (short) 1);
      Assertions.fail();
    } catch (IllegalArgumentException e) {
      Assertions.assertEquals(LightChainAssigner.IDENTIFIER_CANNOT_BE_NULL, e.getMessage());
    }

  }

  /**
   * Tests the assigner confirms it fails and returns IllegalArgumentException when given snapshot is null.
   */
  @Test
  public void testAssignerFails_NullSnapshot() throws IllegalArgumentException {
    // Arrange
    Identifier entityId = IdentifierFixture.newIdentifier();

    // Act
    LightChainAssigner assigner = new LightChainAssigner();
    try {
      assigner.assign(entityId, null, (short) 1);
      Assertions.fail();
    } catch (IllegalArgumentException e) {
      Assertions.assertEquals(LightChainAssigner.SNAPSHOT_CANNOT_BE_NULL, e.getMessage());
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
    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());
    when(snapshot.all()).thenReturn(accounts);

    // Act
    LightChainAssigner assigner = new LightChainAssigner();
    Assignment assignment = assigner.assign(entityId, snapshot, (short) 0);

    // Assert
    Assertions.assertEquals(assignment, new Assignment());
    Assertions.assertEquals(0, assignment.size());
  }
}
