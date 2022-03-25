package protocol.transaction;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import model.crypto.Signature;
import model.lightchain.Account;
import model.lightchain.Identifier;
import model.lightchain.Transaction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import state.Snapshot;
import state.State;
import unittest.fixtures.AccountFixture;
import unittest.fixtures.TransactionFixture;

/**
 * Encapsulates tests for transaction validation part of PoV consensus.
 */
public class ValidatorTest {

  /**
   * Evaluates the transaction validation fails when reference block id points to an invalid (null) snapshot.
   */
  @Test
  public void testTransactionIsNotValid_NullSnapshot() {
    // Arrange
    /// Transaction
    Transaction transaction = TransactionFixture.newTransaction();

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    when(mockState.atBlockId(transaction.getReferenceBlockId())).thenReturn(null);

    /// Verifier
    Validator verifier = new TransactionValidator(mockState);

    // Act
    boolean result = verifier.isCorrect(transaction);

    // Assert
    Assertions.assertFalse(result);
  }

  /**
   * Evaluates the transaction validation fails when sender identifier does not refer to a valid account at the
   * snapshot of reference block.
   */
  @Test
  public void testTransactionIsNotValid_InvalidSender() {
    // Arrange
    /// Transaction
    Transaction transaction = TransactionFixture.newTransaction();

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    Account receiverAccount = new AccountFixture(transaction.getReceiver());

    when(mockState.atBlockId(transaction.getReferenceBlockId())).thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(transaction.getSender())).thenReturn(null);
    when(mockSnapshot.getAccount(transaction.getReceiver())).thenReturn(receiverAccount);

    /// Verifier
    Validator verifier = new TransactionValidator(mockState);

    // Act
    boolean result = verifier.isCorrect(transaction);

    // Assert
    Assertions.assertFalse(result);
  }

  /**
   * Evaluates the transaction validation fails when receiver identifier does not refer to a valid account at the
   * snapshot of reference block.
   */
  @Test
  public void testTransactionIsNotValid_InvalidReceiver() {
    // Arrange
    /// Transaction
    Transaction transaction = TransactionFixture.newTransaction();

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    Account senderAccount = new AccountFixture(transaction.getSender());

    when(mockState.atBlockId(transaction.getReferenceBlockId())).thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(transaction.getSender())).thenReturn(senderAccount);
    when(mockSnapshot.getAccount(transaction.getReceiver())).thenReturn(null);

    /// Verifier
    Validator verifier = new TransactionValidator(mockState);

    // Act
    boolean result = verifier.isCorrect(transaction);

    // Assert
    Assertions.assertFalse(result);
  }

  /**
   * Evaluates the transaction validation fails when the amount sent by receiver is 0.
   */
  @Test
  public void testTransactionIsNotCorrect_ZeroAmount() {
    // Arrange
    /// Transaction
    double amount = 0;
    Transaction transaction = TransactionFixture.newTransaction(amount);

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    Account senderAccount = new AccountFixture(transaction.getSender());
    Account receiverAccount = new AccountFixture(transaction.getReceiver());

    when(mockState.atBlockId(transaction.getReferenceBlockId())).thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(transaction.getSender())).thenReturn(senderAccount);
    when(mockSnapshot.getAccount(transaction.getReceiver())).thenReturn(receiverAccount);

    /// Verifier
    Validator verifier = new TransactionValidator(mockState);

    // Act
    boolean result = verifier.isCorrect(transaction);

    // Assert
    // fixture sanity check
    Assertions.assertEquals(0, transaction.getAmount());

    Assertions.assertFalse(result);
  }

  /**
   * Evaluates the transaction validation fails when the amount sent by receiver is negative.
   */
  @Test
  public void testTransactionIsNotCorrect_NegativeAmount() {
    // Arrange
    /// Transaction
    double amount = -10;
    Transaction transaction = TransactionFixture.newTransaction(amount);

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    Account senderAccount = new AccountFixture(transaction.getSender());
    Account receiverAccount = new AccountFixture(transaction.getReceiver());

    when(mockState.atBlockId(transaction.getReferenceBlockId())).thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(transaction.getSender())).thenReturn(senderAccount);
    when(mockSnapshot.getAccount(transaction.getReceiver())).thenReturn(receiverAccount);

    /// Verifier
    Validator verifier = new TransactionValidator(mockState);

    // Act
    boolean result = verifier.isCorrect(transaction);

    // Assert
    // fixture sanity check
    Assertions.assertTrue(transaction.getAmount() < 0);

    Assertions.assertFalse(result);
  }

  /**
   * Evaluates the transaction validation passes when all below conditions are satisfied:
   * 1- Reference block id is a valid and finalized block.
   * 2- The sender and receiver both refer to valid accounts at the snapshot of the reference block id.
   * 3- Amount value is non-negative.
   *
   */
  @Test
  public void testTransactionIsCorrect() {
    // Arrange
    /// Transaction
    Transaction transaction = TransactionFixture.newTransaction();

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);

    Account senderAccount = new AccountFixture(transaction.getSender());
    Account receiverAccount = new AccountFixture(transaction.getReceiver());

    when(mockState.atBlockId(transaction.getReferenceBlockId())).thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(transaction.getSender())).thenReturn(senderAccount);
    when(mockSnapshot.getAccount(transaction.getReceiver())).thenReturn(receiverAccount);

    /// Verifier
    Validator verifier = new TransactionValidator(mockState);

    // Act
    boolean result = verifier.isCorrect(transaction);

    // Assert
    Assertions.assertTrue(result);
  }

  /**
   * Evaluates the transaction validation fails when the reference block has a lower height than the
   * last block of sender account.
   */
  @Test
  public void testTransactionIsNotSound_LowerHeight() {
    // Arrange
    /// Transaction
    Transaction transaction = TransactionFixture.newTransaction();

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockTransactionSnapshot = mock(Snapshot.class);
    Snapshot mockSenderAccountSnapshot = mock(Snapshot.class);

    Identifier sender = transaction.getSender();
    Account senderAccount = new AccountFixture(sender);

    when(mockState.atBlockId(transaction.getReferenceBlockId())).thenReturn(mockTransactionSnapshot);
    when(mockTransactionSnapshot.getAccount(sender)).thenReturn(senderAccount);
    when(mockState.atBlockId(senderAccount.getLastBlockId())).thenReturn(mockSenderAccountSnapshot);

    when(mockTransactionSnapshot.getReferenceBlockHeight()).thenReturn(1L);
    when(mockSenderAccountSnapshot.getReferenceBlockHeight()).thenReturn(10L);

    /// Verifier
    Validator verifier = new TransactionValidator(mockState);

    // Act
    boolean result = verifier.isSound(transaction);

    // Assert
    Assertions.assertFalse(result);
  }

  /**
   * Evaluates the transaction validation fails when the reference block has an equal height with the
   * last block of sender account.
   */
  @Test
  public void testTransactionIsNotSound_EqualHeight() {
    // Arrange
    /// Transaction
    Transaction transaction = TransactionFixture.newTransaction();

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);

    Identifier sender = transaction.getSender();
    Account senderAccount = new AccountFixture(sender);

    when(mockState.atBlockId(transaction.getReferenceBlockId())).thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(sender)).thenReturn(senderAccount);
    when(mockState.atBlockId(senderAccount.getLastBlockId())).thenReturn(mockSnapshot);
    when(mockSnapshot.getReferenceBlockHeight()).thenReturn(1L);

    /// Verifier
    Validator verifier = new TransactionValidator(mockState);

    // Act
    boolean result = verifier.isSound(transaction);

    // Assert
    Assertions.assertFalse(result);
  }

  /**
   * Evaluates the transaction validation passes when the reference block has a larger height than the
   * last block of sender account.
   */
  @Test
  public void testTransactionIsSound() {
    // Arrange
    /// Transaction
    Transaction transaction = TransactionFixture.newTransaction();

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockTransactionSnapshot = mock(Snapshot.class);
    Snapshot mockSenderAccountSnapshot = mock(Snapshot.class);

    Identifier sender = transaction.getSender();
    Account senderAccount = new AccountFixture(sender);

    when(mockState.atBlockId(transaction.getReferenceBlockId())).thenReturn(mockTransactionSnapshot);
    when(mockTransactionSnapshot.getAccount(sender)).thenReturn(senderAccount);
    when(mockState.atBlockId(senderAccount.getLastBlockId())).thenReturn(mockSenderAccountSnapshot);

    when(mockTransactionSnapshot.getReferenceBlockHeight()).thenReturn(10L);
    when(mockSenderAccountSnapshot.getReferenceBlockHeight()).thenReturn(9L);

    /// Verifier
    Validator verifier = new TransactionValidator(mockState);

    // Act
    boolean result = verifier.isSound(transaction);

    // Assert
    Assertions.assertTrue(result);
  }

  /**
   * Evaluates the transaction validation fails when the transaction signature verification against
   * its sender public key fails.
   */
  @Test
  public void testTransactionIsNotAuthenticated() {
    // Arrange
    /// Transaction
    Transaction transaction = TransactionFixture.newTransaction();

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);

    Identifier sender = transaction.getSender();
    Account senderAccount = new AccountFixture(sender);
    Signature signature = transaction.getSignature();

    when(mockState.atBlockId(transaction.getReferenceBlockId())).thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(sender)).thenReturn(senderAccount);

    when(mockSnapshot.getAccount(sender).getPublicKey().verifySignature(transaction, signature)).thenReturn(false);

    /// Verifier
    Validator verifier = new TransactionValidator(mockState);

    // Act
    boolean result = verifier.isAuthenticated(transaction);

    // Assert
    Assertions.assertFalse(result);
  }

  /**
   * Evaluates the transaction validation passes when the transaction signature verification against
   * its sender public key passes.
   */
  @Test
  public void testTransactionIsAuthenticated() {
    // Arrange
    /// Transaction
    Transaction transaction = TransactionFixture.newTransaction();

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    Identifier sender = transaction.getSender();
    Account senderAccount = new AccountFixture(sender);
    Signature signature = transaction.getSignature();
    when(mockState.atBlockId(transaction.getReferenceBlockId())).thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(sender)).thenReturn(senderAccount);
    when(mockSnapshot.getAccount(sender).getPublicKey().verifySignature(transaction, signature)).thenReturn(true);

    /// Verifier
    Validator verifier = new TransactionValidator(mockState);

    // Act
    boolean result = verifier.isAuthenticated(transaction);

    // Assert
    Assertions.assertTrue(result);
  }

  /**
   * Evaluates the transaction validation fails when the sender balance has a lower amount than the transaction amount.
   */
  @Test
  public void testSenderDoesNotHasEnoughBalance() {
    // Arrange
    /// Transaction
    Transaction transaction = TransactionFixture.newTransaction();

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    Identifier sender = transaction.getSender();
    Account senderAccount = new AccountFixture(sender);
    senderAccount.setBalance(0);

    when(mockState.atBlockId(transaction.getReferenceBlockId())).thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(sender)).thenReturn(senderAccount);

    /// Verifier
    Validator verifier = new TransactionValidator(mockState);

    // Act
    boolean result = verifier.senderHasEnoughBalance(transaction);

    // Assert
    Assertions.assertFalse(result);
  }

  /**
   * Evaluates the transaction validation fails when the sender balance
   * has a greater amount than the transaction amount.
   */
  @Test
  public void testSenderHasEnoughBalance() {
    // Arrange
    /// Transaction
    Transaction transaction = TransactionFixture.newTransaction();

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    Identifier sender = transaction.getSender();
    Account senderAccount = new AccountFixture(sender);
    // sender always having more balance than transaction amount
    senderAccount.setBalance(transaction.getAmount() + 1000);

    when(mockState.atBlockId(transaction.getReferenceBlockId())).thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(sender)).thenReturn(senderAccount);

    /// Verifier
    Validator verifier = new TransactionValidator(mockState);

    // Act
    boolean result = verifier.senderHasEnoughBalance(transaction);

    // Assert
    Assertions.assertTrue(result);
  }

}
