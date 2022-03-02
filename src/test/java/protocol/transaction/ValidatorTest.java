package protocol.transaction;

import model.lightchain.Account;
import model.lightchain.Identifier;
import model.lightchain.Transaction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import state.Snapshot;
import state.State;
import unittest.fixtures.AccountFixture;
import unittest.fixtures.IdentifierFixture;
import unittest.fixtures.TransactionFixture;

import java.util.Random;

import static org.mockito.Mockito.*;
public class ValidatorTest {
  // Note: except actual implementation of Validator, mock everything else, and use fixtures when needed.
  //
  // TODO: a single individual test function for each of these scenarios:
  Random random = new Random();
  // 1. isCorrect fails since reference block id does not represent a valid snap shot (i.e., null snapshot).
  @Test
  public void isCorrectFail_NullSnapshot(){
    // Arrange
    /// Identifiers
    Transaction transaction = TransactionFixture.NewTransaction();

    /// State & Snapshot
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    //when(mockState.atBlockId(referenceBlockId)).thenReturn(mockSnapshot);
   // when(mockSnapshot.getAccount(sender))

    Validator verifier = new TransactionVerifier();

    // Act
    boolean result = verifier.isCorrect(transaction);

    // Assert
    Assertions.assertFalse(result);
  }

  // 2. isCorrect fails since sender does not refer to a valid account at the snapshot of reference block.
  // 3. isCorrect fails since receiver does not refer to a valid account at the snapshot of reference block.
  // 4. isCorrect fails since amount is negative (and also a case for zero).
  // 5. isCorrect passes when all conditions satisfied.
  @Test
  public void isCorrectPass(){
    // Arrange
    /// Identifiers
    Transaction transaction = TransactionFixture.NewTransaction();
    Identifier sender = transaction.getSender();
    Account senderAccount = new AccountFixture(sender);
    Identifier receiver = transaction.getReceiver();
    Account receiverAccount = new AccountFixture(receiver);
    Identifier referenceBlockID = transaction.getReferenceBlockId();
    /// State & Snapshot
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    when(mockState.atBlockId(referenceBlockID)).thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(sender)).thenReturn(senderAccount);
    when(mockSnapshot.getAccount(receiver)).thenReturn(receiverAccount);

    // Act
    Validator verifier = new TransactionVerifier();
    boolean result = verifier.isCorrect(transaction);
    Assertions.assertTrue(result);
  }

  //
  // 6. isSound fails since reference block has a lower height (and also case for equal) than the last block of sender account.
  @Test
  public void isSoundFail_LowerHeight(){
    // Arrange
    /// Identifiers
    Transaction transaction = TransactionFixture.NewTransaction();
    Identifier sender = transaction.getSender();
    Account senderAccount = new AccountFixture(sender);
    Identifier referenceBlockID = transaction.getReferenceBlockId();
    /// State & Snapshot
    Identifier mockLastBlockID = mock(Identifier.class);
    Snapshot mockSenderSnapshot = mock(Snapshot.class);
    byte[] mockBytes = new byte[32];
    random.nextBytes(mockBytes);
    long mockLastHeight = random.nextLong()+10;
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    when(mockState.atBlockId(referenceBlockID)).thenReturn(mockSnapshot);
    when(mockSnapshot.getReferenceBlockHeight()).thenReturn(random.nextLong());
    when(mockSnapshot.getAccount(sender)).thenReturn(senderAccount);

    when(mockSenderSnapshot.getReferenceBlockId()).thenReturn(mockLastBlockID);
    when(mockSenderSnapshot.getReferenceBlockHeight()).thenReturn(mockLastHeight);
    // Act
    Validator verifier = new TransactionVerifier();
    boolean result = verifier.isSound(transaction);

    // Assert
    Assertions.assertFalse(result);
  }
  // 7. isSound passes since reference block has a higher height than the last block of the sender account.
  @Test
  public void isSoundPass(){
    // Arrange
    /// Identifiers
    Transaction transaction = TransactionFixture.NewTransaction();
    Identifier sender = transaction.getSender();
    Account senderAccount = new AccountFixture(sender);
    Identifier referenceBlockID = transaction.getReferenceBlockId();
    /// State & Snapshot
    Identifier mockLastBlockID = mock(Identifier.class);
    Snapshot mockSenderSnapshot = mock(Snapshot.class);
    byte[] mockBytes = new byte[32];
    random.nextBytes(mockBytes);
    long mockLastHeight = random.nextLong();
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    when(mockState.atBlockId(referenceBlockID)).thenReturn(mockSnapshot);
    when(mockSnapshot.getReferenceBlockHeight()).thenReturn(random.nextLong()+10);
    when(mockSnapshot.getAccount(sender)).thenReturn(senderAccount);

    when(mockSenderSnapshot.getReferenceBlockId()).thenReturn(mockLastBlockID);
    when(mockSenderSnapshot.getReferenceBlockHeight()).thenReturn(mockLastHeight);
    // Act
    Validator verifier = new TransactionVerifier();
    boolean result = verifier.isSound(transaction);

    // Assert
    Assertions.assertTrue(result);
  }
  // 8. isAuthenticated fails since transaction signature verification against its sender public key fails.
  // 9. isAuthenticated passes when transaction signature verification against its sender public key passes.
  //
  // 10. senderHasEnoughBalance fails when sender has a balance lower than transaction amount.
  @Test
  public void senderHasEnoughBalanceFail(){
    // Arrange
    /// Identifiers
    Transaction transaction = TransactionFixture.NewTransaction();
    Identifier sender = transaction.getSender();
    Account senderAccount = new AccountFixture(sender);
    Identifier referenceBlockID = transaction.getReferenceBlockId();

    /// State & Snapshot
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    when(mockState.atBlockId(referenceBlockID)).thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(sender)).thenReturn(senderAccount);

    // Act
    Validator verifier = new TransactionVerifier();
    boolean result = verifier.senderHasEnoughBalance(transaction);
    Assertions.assertFalse(result);
    //
  }
  // 11. senderHasEnoughBalance passes when sender has a balance greater than or equal to the transaction amount.
  @Test
  public void senderHasEnoughBalancePass(){
    // Arrange
    /// Identifiers
    Transaction transaction = TransactionFixture.NewTransaction();
    Identifier sender = transaction.getSender();
    Account senderAccount = new AccountFixture(sender);
    Identifier referenceBlockID = transaction.getReferenceBlockId();
    // TODO: don't we need a balance setter function for the accounts?
    // They all start with zero balance with no setters to use. Since AccountFixture uses Account constructor,
    // balance is always zero so this test will fail at this moment.

    /// State & Snapshot
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    when(mockState.atBlockId(referenceBlockID)).thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(sender)).thenReturn(senderAccount);

    // Act
    Validator verifier = new TransactionVerifier();
    boolean result = verifier.senderHasEnoughBalance(transaction);
    Assertions.assertTrue(result);
    //
  }

}
