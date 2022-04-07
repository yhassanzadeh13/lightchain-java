package state.table;

import java.util.*;

import static org.mockito.Mockito.*;

import model.lightchain.Account;
import model.lightchain.Identifier;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import state.Snapshot;
import unittest.fixtures.AccountFixture;
import unittest.fixtures.IdentifierFixture;

/**
 * Encapsulates tests for hash table implementation of protocol state.
 */
public class TableStateTest {
  /**
   * Snapshots list for 10 snapshots.
   */
  protected ArrayList<Snapshot> snapshots = new ArrayList<>();
  private static final Random random = new Random();

  @Test
  public void testTableState() {
    // Arrange
    /// State and Snapshots
    snapshotsListSetter();
    TableState tableState = new TableState();
    ArrayList<Identifier> refBlockIds = new ArrayList<>();
    Pair<Identifier, Long> maxHeightSnapshot = Pair.of(null, 0L);

    /// Accounts
    for (int i = 0; i < 10; i++) {
      Identifier referenceBlockId = IdentifierFixture.newIdentifier();
      long blockHeight = (long) (random.nextDouble() * 100L);
      if (blockHeight > maxHeightSnapshot.getRight()) {
        maxHeightSnapshot = Pair.of(referenceBlockId, blockHeight);
      }
      when(snapshots.get(i).getReferenceBlockId()).thenReturn(referenceBlockId);
      when(snapshots.get(i).getReferenceBlockHeight()).thenReturn(blockHeight);
      HashMap<Identifier, Account> accounts = AccountFixture.newAccounts(10, 10);
      for (Map.Entry<Identifier, Account> set : accounts.entrySet()) {
        when(snapshots.get(i).getAccount(set.getKey())).thenReturn(set.getValue());
      }
      when(snapshots.get(i).all()).thenReturn(new ArrayList<>(accounts.values()));
      tableState.addSnapshot(referenceBlockId, snapshots.get(i));
      refBlockIds.add(referenceBlockId);
    }

    // Act
    boolean lastSnapshotCorrectness = tableState.last().equals(tableState.atBlockId(maxHeightSnapshot.getLeft()));

    // Assert
    for (int j = 0; j < snapshots.size(); j++) {
      Assertions.assertEquals(tableState.atBlockId(refBlockIds.get(j)), snapshots.get(j));
      for (Account account : snapshots.get(j).all()) {
        Assertions.assertEquals(snapshots.get(j).getAccount(account.getIdentifier()), account);
      }
    }
    Assertions.assertTrue(lastSnapshotCorrectness);
  }

  /**
   * Creates 10 mock snapshots and adds them to the global snapshots list.
   */
  public void snapshotsListSetter() {
    Snapshot snapshot = mock(Snapshot.class);
    Snapshot snapshot2 = mock(Snapshot.class);
    Snapshot snapshot3 = mock(Snapshot.class);
    Snapshot snapshot4 = mock(Snapshot.class);
    Snapshot snapshot5 = mock(Snapshot.class);
    Snapshot snapshot6 = mock(Snapshot.class);
    Snapshot snapshot7 = mock(Snapshot.class);
    Snapshot snapshot8 = mock(Snapshot.class);
    Snapshot snapshot9 = mock(Snapshot.class);
    Snapshot snapshot10 = mock(Snapshot.class);
    snapshots.add(snapshot);
    snapshots.add(snapshot2);
    snapshots.add(snapshot3);
    snapshots.add(snapshot4);
    snapshots.add(snapshot5);
    snapshots.add(snapshot6);
    snapshots.add(snapshot7);
    snapshots.add(snapshot8);
    snapshots.add(snapshot9);
    snapshots.add(snapshot10);
  }
}
