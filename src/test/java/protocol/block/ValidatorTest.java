package protocol.block;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import model.crypto.Signature;
import model.lightchain.Account;
import model.lightchain.Block;
import model.lightchain.Identifier;
import model.lightchain.ValidatedTransaction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import protocol.Parameters;
import state.Snapshot;
import state.State;
import unittest.fixtures.AccountFixture;
import unittest.fixtures.BlockFixture;
import unittest.fixtures.IdentifierFixture;
import unittest.fixtures.ValidatedTransactionFixture;

/**
 * Encapsulates tests for block validation part of PoV consensus.
 */
public class ValidatorTest {
  /**
   * Evaluates the block validation fails when previous block id
   * does not represent a valid snapshot (i.e., null snapshot).
   */
  @Test
  public void testBlockIsNotCorrect_InvalidPreviousBlockSnapshot() {
    // Arrange
    /// Block
    Block block = BlockFixture.newBlock();

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    when(mockState.atBlockId(block.getPreviousBlockId())).thenReturn(null);

    /// Verifier
    InfBlockValidator validator = new BlockValidator(mockState);

    // Act
    boolean result = validator.isCorrect(block);

    // Assert
    Assertions.assertFalse(result);
  }

  /**
   * Evaluates the block validation fails when proposer does
   * not refer to a valid account at the snapshot of previous block id.
   */
  @Test
  public void testBlockIsNotCorrect_InvalidProposer() {
    // Arrange
    /// Block
    Block block = BlockFixture.newBlock();

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    when(mockState.atBlockId(block.getPreviousBlockId())).thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(block.getProposer())).thenReturn(null);

    /// Verifier
    InfBlockValidator validator = new BlockValidator(mockState);

    // Act
    boolean result = validator.isCorrect(block);

    // Assert
    Assertions.assertFalse(result);
  }

  /**
   * Evaluates the block validation fails when number of
   * validated transactions included in the block are below min threshold.
   */
  @Test
  public void testBlockIsNotCorrect_ValidatedTransactionBelowMinimum() {
    // Arrange
    /// Block
    int validatedTransactionSize = Parameters.MIN_TRANSACTIONS_NUM - 1;
    Assertions.assertTrue(validatedTransactionSize >= 0);

    Block block = BlockFixture.newBlock(validatedTransactionSize);

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);

    Identifier proposer = block.getProposer();
    Account proposerAccount = AccountFixture.newAccount(proposer);

    when(mockState.atBlockId(block.getPreviousBlockId())).thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(proposer)).thenReturn(proposerAccount);

    /// Verifier
    InfBlockValidator validator = new BlockValidator(mockState);

    // Act
    boolean result = validator.isCorrect(block);

    // Assert
    Assertions.assertFalse(result);
  }

  /**
   * Evaluates the block validation fails when number of
   * validated transactions included in the block are above max threshold.
   */
  @Test
  public void testBlockIsNotCorrect_ValidatedTransactionAboveMaximum() {
    // Arrange
    /// Block
    int validatedTransactionSize = Parameters.MAX_TRANSACTIONS_NUM + 2;
    Block block = BlockFixture.newBlock(validatedTransactionSize);

    // State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);

    Identifier proposer = block.getProposer();
    Account proposerAccount = AccountFixture.newAccount(proposer);

    when(mockState.atBlockId(block.getPreviousBlockId())).thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(proposer)).thenReturn(proposerAccount);

    /// Verifier
    InfBlockValidator validator = new BlockValidator(mockState);

    // Act
    boolean result = validator.isCorrect(block);

    // Assert
    Assertions.assertFalse(result);
  }

  /**
   * Evaluates the block validation passes when all conditions, i.e.
   * 1- Previous block id is a valid and finalized block,
   * 2- Proposer refers to a valid identity at the snapshot of the previous block id,
   * 3- the number of transactions are within the permissible range of LightChain parameters.
   * satisfied.
   */
  @Test
  public void testBlockIsCorrect() {
    // Arrange
    /// Block
    int validatedTransactionSize = (Parameters.MIN_TRANSACTIONS_NUM + Parameters.MAX_TRANSACTIONS_NUM) / 2;
    Assertions.assertNotEquals(0, validatedTransactionSize);
    Block block = BlockFixture.newBlock(validatedTransactionSize);

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);

    Identifier proposer = block.getProposer();
    Account proposerAccount = AccountFixture.newAccount(proposer);

    when(mockState.atBlockId(block.getPreviousBlockId())).thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(proposer)).thenReturn(proposerAccount);

    /// Verifier
    InfBlockValidator validator = new BlockValidator(mockState);

    // Act
    boolean result = validator.isCorrect(block);

    // Assert
    Assertions.assertTrue(result);
  }

  /**
   * Evaluates the block validation fails hen previous block id
   * does not refer to the latest snapshot of the validating node.
   */
  @Test
  public void testBlockIsNotConsistent_InvalidPreviousBlockId() {
    // Arrange
    /// Block
    Block block = BlockFixture.newBlock();

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    Identifier dummyReferenceBlockId = IdentifierFixture.newIdentifier();
    when(mockState.last()).thenReturn(mockSnapshot);
    when(mockSnapshot.getReferenceBlockId()).thenReturn(dummyReferenceBlockId);

    /// Verifier
    InfBlockValidator validator = new BlockValidator(mockState);

    // Act
    boolean result = validator.isConsistent(block);

    // Assert
    Assertions.assertFalse(result);
  }

  /**
   * Evaluates the block validation passes when previous block id refers to the latest snapshot of the validating node.
   */
  @Test
  public void testBlockIsConsistent() {
    //Arrange
    /// Block
    Block block = BlockFixture.newBlock();

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    when(mockState.atBlockId(block.getPreviousBlockId())).thenReturn(mockSnapshot);
    when(mockState.last()).thenReturn(mockSnapshot);
    when(mockSnapshot.getReferenceBlockId()).thenReturn(block.getPreviousBlockId());

    ///Verifier
    InfBlockValidator validator = new BlockValidator(mockState);

    //Act
    boolean result = validator.isConsistent(block);

    //Assert
    Assertions.assertTrue(result);
  }

  /**
   * Evaluates the block validation fails since block signature verification against its proposer public key fails.
   */
  @Test
  public void testBlockIsNotAuthenticated() {
    // Arrange
    /// Block
    Block block = BlockFixture.newBlock();

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    Identifier proposer = block.getProposer();
    Account proposerAccount = AccountFixture.newAccount(proposer);
    when(mockState.atBlockId(block.getPreviousBlockId()))
        .thenReturn(mockSnapshot);
    when(mockSnapshot
        .getAccount(proposer))
        .thenReturn(proposerAccount);
    when(mockSnapshot.getAccount(proposer)
        .getPublicKey()
        .verifySignature(block, block.getProposerSignature())).thenReturn(false);

    /// Verifier
    InfBlockValidator validator = new BlockValidator(mockState);

    // Act
    boolean result = validator.isAuthenticated(block);

    // Assert
    Assertions.assertFalse(result);
  }

  /**
   * Evaluates the block validation passes since block signature verification against its proposer public key passes.
   */
  @Test
  public void testBlockIsAuthenticated() {
    // Arrange
    /// Block
    Block block = BlockFixture.newBlock();

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    Identifier proposer = block.getProposer();
    Account proposerAccount = AccountFixture.newAccount(proposer);
    when(mockState.atBlockId(block.getPreviousBlockId()))
        .thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(proposer))
        .thenReturn(proposerAccount);
    when(mockSnapshot.getAccount(proposer)
        .getPublicKey()
        // TODO: change to exact block once we have header and body decoupled.
        .verifySignature(any(Block.class), eq(block.getProposerSignature())))
        .thenReturn(true);

    /// Verifier
    InfBlockValidator verifier = new BlockValidator(mockState);

    // Act
    boolean result = verifier.isAuthenticated(block);

    // Assert
    Assertions.assertTrue(result);
  }

  /**
   * Evaluates block validation fails when proposer has a stake lower than minimum required stakes.
   */
  @Test
  public void testProposerHasNotEnoughStake() {
    // Arrange
    /// Block
    Block block = BlockFixture.newBlock();

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    Identifier proposer = block.getProposer();
    Account proposerAccount = AccountFixture.newAccount(proposer, Parameters.MINIMUM_STAKE - 2);
    when(mockState.atBlockId(block.getPreviousBlockId())).thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(proposer)).thenReturn(proposerAccount);

    /// Verifier
    InfBlockValidator validator = new BlockValidator(mockState);

    // Act
    boolean result = validator.proposerHasEnoughStake(block);

    // Assert
    Assertions.assertFalse(result);
  }

  /**
   * Evaluates the block validation passes when proposer has enough amount
   * of stake greater than or equal to minimum required one.
   */
  @Test
  public void testProposerHasEnoughStake() {
    // Arrange
    /// Block
    Block block = BlockFixture.newBlock();

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    Identifier proposer = block.getProposer();
    Account proposerAccount = AccountFixture.newAccount(proposer);
    when(mockState.atBlockId(block.getPreviousBlockId())).thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(proposer)).thenReturn(proposerAccount);

    /// Verifier
    InfBlockValidator validator = new BlockValidator(mockState);

    // Act
    boolean result = validator.proposerHasEnoughStake(block);

    // Assert
    Assertions.assertTrue(result);
  }

  /**
   * Evaluates the block validation fails when there is at least one transaction that does not have a minimum number of
   * certificates from staked validators that pass the signature verification.
   */
  @Test
  public void testBlockAllTransactionsNotValidated() {
    // Arrange
    /// Block
    ValidatedTransaction transaction1 = ValidatedTransactionFixture
        .newValidatedTransaction(
            IdentifierFixture.newIdentifier(),
            IdentifierFixture.newIdentifier(),
            Parameters.SIGNATURE_THRESHOLD - 1);
    ValidatedTransaction transaction2 = ValidatedTransactionFixture
        .newValidatedTransaction(
            IdentifierFixture.newIdentifier(),
            IdentifierFixture.newIdentifier(),
            Parameters.SIGNATURE_THRESHOLD);

    Block block = BlockFixture.newBlock(new ValidatedTransaction[]{transaction1, transaction2});

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    when(mockState.atBlockId(block.getPreviousBlockId())).thenReturn(mockSnapshot);

    // mocks transactions validators accounts and signatures.
    for (ValidatedTransaction transaction : block.getTransactions()) {
      when(mockSnapshot.getAccount(transaction.getSender()))
          .thenReturn(AccountFixture.newAccount(transaction.getSender()));
      for (Signature signature : transaction.getCertificates()) {
        when(mockSnapshot.getAccount(signature.getSignerId()))
            .thenReturn(AccountFixture.newAccount(signature.getSignerId()));
      }
    }

    /// Verifier
    InfBlockValidator validator = new BlockValidator(mockState);

    // Act
    boolean result = validator.allTransactionsValidated(block);

    // Assert
    Assertions.assertFalse(result);
  }

  /**
   * Evaluates the block validation passes when there all transactions have a minimum number of
   * certificates from staked validators, and all certificates pass the signature verification.
   */
  @Test
  public void testBlockAllTransactionsValidated() {
    // Arrange
    /// Block
    Block block = BlockFixture.newBlock();

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    when(mockState.atBlockId(block.getPreviousBlockId())).thenReturn(mockSnapshot);

    // mocks transactions validators accounts and signatures.
    for (ValidatedTransaction transaction : block.getTransactions()) {
      when(mockSnapshot.getAccount(transaction.getSender()))
          .thenReturn(AccountFixture.newAccount(transaction.getSender()));
      for (Signature signature : transaction.getCertificates()) {
        when(mockSnapshot.getAccount(signature.getSignerId()))
            .thenReturn(AccountFixture.newAccount(signature.getSignerId()));
      }
    }

    ///Verifier
    InfBlockValidator validator = new BlockValidator(mockState);

    //Act
    boolean result = validator.allTransactionsValidated(block);

    //Assert
    Assertions.assertTrue(result);
  }

  /**
   * Evaluates the block validation fails when there is at least one transaction that fails on its soundness.
   */
  @Test
  public void testBlockAllTransactionsNotSound() {
    // Arrange
    /// Block
    Block block = BlockFixture.newBlock();

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    when(mockState.atBlockId(block.getPreviousBlockId())).thenReturn(mockSnapshot);

    // assumes first transaction of the block is not sound.
    ValidatedTransaction transaction = block.getTransactions()[0];
    Snapshot mockTransactionSnapshot = mock(Snapshot.class);
    Snapshot mockSenderAccountSnapshot = mock(Snapshot.class);

    Identifier sender = transaction.getSender();
    Account senderAccount = AccountFixture.newAccount(sender);

    when(mockState.atBlockId(transaction.getReferenceBlockId())).thenReturn(mockTransactionSnapshot);
    when(mockTransactionSnapshot.getAccount(sender)).thenReturn(senderAccount);
    when(mockState.atBlockId(senderAccount.getLastBlockId())).thenReturn(mockSenderAccountSnapshot);
    // mocks sender has a higher last block height than block height that transaction refers to.
    when(mockTransactionSnapshot.getReferenceBlockHeight()).thenReturn(1L);
    when(mockSenderAccountSnapshot.getReferenceBlockHeight()).thenReturn(10L);

    /// Verifier
    InfBlockValidator verifier = new BlockValidator(mockState);

    // Act
    boolean result = verifier.allTransactionsSound(block);

    // Assert
    Assertions.assertFalse(result);
  }

  /**
   * Evaluates the block validation passes when all transactions pass on their soundness.
   */
  @Test
  public void testBlockAllTransactionsSound() {
    // Arrange
    /// Block
    Block block = BlockFixture.newBlock();

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    when(mockState.atBlockId(block.getPreviousBlockId())).thenReturn(mockSnapshot);

    for (ValidatedTransaction transaction : block.getTransactions()) {
      Snapshot mockTransactionSnapshot = mock(Snapshot.class);
      Snapshot mockSenderAccountSnapshot = mock(Snapshot.class);

      Identifier sender = transaction.getSender();
      Account senderAccount = AccountFixture.newAccount(sender);

      // mocks sender has a lower last block height than the block height that transaction refers to.
      when(mockState.atBlockId(transaction.getReferenceBlockId())).thenReturn(mockTransactionSnapshot);
      when(mockTransactionSnapshot.getAccount(sender)).thenReturn(senderAccount);
      when(mockTransactionSnapshot.getReferenceBlockHeight()).thenReturn(10L);

      when(mockState.atBlockId(senderAccount.getLastBlockId())).thenReturn(mockSenderAccountSnapshot);
      when(mockSenderAccountSnapshot.getReferenceBlockHeight()).thenReturn(1L);

    }

    /// Verifier
    InfBlockValidator validator = new BlockValidator(mockState);

    // Act
    boolean result = validator.allTransactionsSound(block);

    // Assert
    Assertions.assertTrue(result);
  }

  /**
   * Evaluates the block validation fails when there is at least
   * two distinct transactions in a block that share the same sender.
   */
  @Test
  public void testBlockDuplicateSender() {
    // Arrange
    /// Block
    // creates two transactions with the same sender.
    Identifier sender = IdentifierFixture.newIdentifier();
    ValidatedTransaction transaction1 = ValidatedTransactionFixture.newValidatedTransaction(sender);
    ValidatedTransaction transaction2 = ValidatedTransactionFixture.newValidatedTransaction(sender);
    ValidatedTransaction[] transactions = new ValidatedTransaction[]{transaction1, transaction2};

    Block block = BlockFixture.newBlock(transactions);

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    when(mockState.atBlockId(block.getPreviousBlockId())).thenReturn(mockSnapshot);

    /// Verifier
    InfBlockValidator validator = new BlockValidator(mockState);

    // Act
    boolean result = validator.noDuplicateSender(block);

    // Assert
    Assertions.assertFalse(result);
  }

  /**
   * Evaluates the block validation passes when all distinct transaction in a block have distinct senders.
   */
  @Test
  public void testBlockNoDuplicateSender() {
    // Arrange
    /// Block
    // creates two transactions with distinct senders.
    Identifier sender = IdentifierFixture.newIdentifier();
    Identifier sender2 = IdentifierFixture.newIdentifier();
    ValidatedTransaction transaction1 = ValidatedTransactionFixture.newValidatedTransaction(sender);
    ValidatedTransaction transaction2 = ValidatedTransactionFixture.newValidatedTransaction(sender2);
    ValidatedTransaction[] transactions = new ValidatedTransaction[]{transaction1, transaction2};

    Block block = BlockFixture.newBlock(transactions);

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    when(mockState.atBlockId(block.getPreviousBlockId())).thenReturn(mockSnapshot);

    /// Verifier
    InfBlockValidator verifier = new BlockValidator(mockState);

    // Act
    boolean result = verifier.noDuplicateSender(block);

    // Assert
    Assertions.assertTrue(result);
  }
}
