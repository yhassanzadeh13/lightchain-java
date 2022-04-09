package state.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
  private static final Random random = new Random();

  @Test
  public void testTableState() {
    // Arrange
    /// State and Snapshots
    ArrayList<Snapshot> snapshots = mockSnapshots(10);
    TableState tableState = new TableState();
    ArrayList<Identifier> refBlockIds = new ArrayList<>();
    Pair<Identifier, Long> maxHeightSnapshot = Pair.of(null, 0L);

    /// Creates 10 snapshots each with 10 staked and 10 unstaked accounts
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

    // checks last snapshot being the one with the greatest block height.
    Assertions.assertEquals(tableState.last(), tableState.atBlockId(maxHeightSnapshot.getLeft()));


    // checks accounts per snapshots.
    for (int j = 0; j < snapshots.size(); j++) {
      Snapshot snapshot = tableState.atBlockId(refBlockIds.get(j));
      Assertions.assertEquals(snapshot, snapshots.get(j));
      for (Account account : snapshot.all()) {
        Assertions.assertEquals(snapshots.get(j).getAccount(account.getIdentifier()), account);
      }
    }

  }

  /**
   * Creates and returns an arraylist of mock snapshots.
   *
   * @param count total number of mock snapshots.
   * @return arraylist of mock snapshots.
   */
  public ArrayList<Snapshot> mockSnapshots(int count) {
    ArrayList<Snapshot> snapshots = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Snapshot snapshot = mock(Snapshot.class);
      snapshots.add(snapshot);
    }

    return snapshots;
  }
}
