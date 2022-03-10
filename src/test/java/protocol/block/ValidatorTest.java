package protocol.block;

import model.lightchain.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import protocol.Parameters;
import protocol.transaction.TransactionVerifier;
import state.Snapshot;
import state.State;
import unittest.fixtures.*;

import java.util.Random;

import static org.mockito.Mockito.*;

public class ValidatorTest {
  /**
   * Random object to create random integers.
   */
  private static final Random random = new Random();

  // Note: except actual implementation of block Validator, mock everything else, and use fixtures when needed.
  //
  // TODO: a single individual test function for each of these scenarios:
  // 1. isCorrect fails since previous block id does not represent a valid snap shot (i.e., null snapshot).
  @Test
  public void testBlockIsNotCorrect_InvalidPreviousBlockSnapshot() {
    //Arrange
    /// Block
    Block block = BlockFixture.newBlock();

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    when(mockState.atBlockId(block.getPreviousBlockId())).thenReturn(null);

    ///Verifier
    Validator verifier = new BlockVerifier(mockState);

    //Act
    boolean result = verifier.isCorrect(block);

    //Assert
    Assertions.assertFalse(result);
  }

  // 2. isCorrect fails when proposer does not refer to a valid account at the snapshot of previous block id.
  @Test
  public void testBlockIsNotCorrect_InvalidProposer() {
    //Arrange
    /// Block
    Block block = BlockFixture.newBlock();

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    when(mockState.atBlockId(block.getPreviousBlockId())).thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(block.getProposer())).thenReturn(null);

    ///Verifier
    Validator verifier = new BlockVerifier(mockState);

    //Act
    boolean result = verifier.isCorrect(block);

    //Assert
    Assertions.assertFalse(result);
  }

  // 3. isCorrect fails when number of validated transactions included in the block are below min threshold.
  @Test
  public void testBlockIsNotCorrect_ValidatedTransactionBelowMinimum() {
    //Arrange
    /// Block
    int validatedTransactionSize = Parameters.MIN_TRANSACTIONS_NUM - 2;
    validatedTransactionSize = validatedTransactionSize < 0 ? 0 : validatedTransactionSize;
    Block block = BlockFixture.newBlock(validatedTransactionSize);

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);

    Identifier proposer = block.getProposer();
    Account proposerAccount = new AccountFixture(proposer);

    when(mockState.atBlockId(block.getPreviousBlockId())).thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(proposer)).thenReturn(proposerAccount);

    ///Verifier
    Validator verifier = new BlockVerifier(mockState);

    //Act
    boolean result = verifier.isCorrect(block);

    //Assert
    Assertions.assertFalse(result);
  }

  // 4. isCorrect fails when number of validated transactions included in the block are above max threshold
  @Test
  public void testBlockIsNotCorrect_ValidatedTransactionAboveMaximum() {
    //Arrange
    /// Block
    int validatedTransactionSize = Parameters.MAX_TRANSACTIONS_NUM + 2;
    Block block = BlockFixture.newBlock(validatedTransactionSize);

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);

    Identifier proposer = block.getProposer();
    Account proposerAccount = new AccountFixture(proposer);

    when(mockState.atBlockId(block.getPreviousBlockId())).thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(proposer)).thenReturn(proposerAccount);

    ///Verifier
    Validator verifier = new BlockVerifier(mockState);

    //Act
    boolean result = verifier.isCorrect(block);

    //Assert
    Assertions.assertFalse(result);
  }

  // 5. isCorrect passes when all conditions satisfied.
  @Test
  public void testBlockIsCorrect() {
    //Arrange
    /// Block
    int validatedTransactionSize = (Parameters.MIN_TRANSACTIONS_NUM + Parameters.MAX_TRANSACTIONS_NUM) / 2;
    Block block = BlockFixture.newBlock(validatedTransactionSize);

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);

    Identifier proposer = block.getProposer();
    Account proposerAccount = new AccountFixture(proposer);

    when(mockState.atBlockId(block.getPreviousBlockId())).thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(proposer)).thenReturn(proposerAccount);

    ///Verifier
    Validator verifier = new BlockVerifier(mockState);

    //Act
    boolean result = verifier.isCorrect(block);

    //Assert
    Assertions.assertTrue(result);
  }

  //
  // 6. isConsistent fails when previous block id does not refer to the latest snapshot of the validating node.
  @Test
  public void testBlockIsNotConsistent_InvalidPreviousBlockId() {
    //Arrange
    /// Block
    Block block = BlockFixture.newBlock();

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    Snapshot mockSnapshot2 = mock(Snapshot.class);
    Identifier dummyReferenceBlockId = IdentifierFixture.newIdentifier();
    when(mockState.atBlockId(block.getPreviousBlockId())).thenReturn(mockSnapshot);
    when(mockState.atBlockId(dummyReferenceBlockId)).thenReturn(mockSnapshot2);
    when(mockState.last()).thenReturn(mockSnapshot2);

    ///Verifier
    Validator verifier = new BlockVerifier(mockState);

    //Act
    boolean result = verifier.isConsistent(block);

    //Assert
    Assertions.assertFalse(result);
  }

  // 7. isConsistent passes when previous block id refers to the latest snapshot of the validating node.
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
    when(mockState.last().getReferenceBlockId()).thenReturn(block.getPreviousBlockId());

    ///Verifier
    Validator verifier = new BlockVerifier(mockState);

    //Act
    boolean result = verifier.isConsistent(block);

    //Assert
    Assertions.assertTrue(result);
  }

  //
  // 8. isAuthenticated fails since block signature verification against its proposer public key fails.
  @Test
  public void testBlockIsNotAuthenticated() {
    //Arrange
    /// Block
    Block block = BlockFixture.newBlock();

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    Identifier proposer = block.getProposer();
    Account proposerAccount = new AccountFixture(proposer);
    when(mockState.atBlockId(block.getPreviousBlockId())).thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(proposer)).thenReturn(proposerAccount);
    when(mockSnapshot.getAccount(proposer).getPublicKey().verifySignature(block, block.getSignature())).thenReturn(false);

    ///Verifier
    Validator verifier = new BlockVerifier(mockState);

    //Act
    boolean result = verifier.isAuthenticated(block);

    //Assert
    Assertions.assertFalse(result);
  }

  // 9. isAuthenticated passes when block signature verification against its proposer public key passes.
  @Test
  public void testBlockIsAuthenticated() {
    //Arrange
    /// Block
    Block block = BlockFixture.newBlock();

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    Identifier proposer = block.getProposer();
    Account proposerAccount = new AccountFixture(proposer);
    when(mockState.atBlockId(block.getPreviousBlockId())).thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(proposer)).thenReturn(proposerAccount);
    when(mockSnapshot.getAccount(proposer).getPublicKey().verifySignature(block, block.getSignature())).thenReturn(true);

    ///Verifier
    Validator verifier = new BlockVerifier(mockState);

    //Act
    boolean result = verifier.isAuthenticated(block);

    //Assert
    Assertions.assertTrue(result);
  }

  //
  // 10. proposerHashEnoughStake fails when proposer has a stake lower than minimum required stakes.
  @Test
  public void testProposerHasNotEnoughStake() {
    //Arrange
    /// Block
    Block block = BlockFixture.newBlock();

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    Identifier proposer = block.getProposer();
    Account proposerAccount = new AccountFixture(proposer, Parameters.MINIMUM_STAKE - 2);
    when(mockState.atBlockId(block.getPreviousBlockId())).thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(proposer)).thenReturn(proposerAccount);

    ///Verifier
    Validator verifier = new BlockVerifier(mockState);

    //Act
    boolean result = verifier.proposerHasEnoughStake(block);

    //Assert
    Assertions.assertFalse(result);
  }

  // 11. proposerHasEnoughStake passes when proposer has enough amount of stake greater than or equal to minimum required one.
  @Test
  public void testProposerHasEnoughStake() {
    //Arrange
    /// Block
    Block block = BlockFixture.newBlock();

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    Identifier proposer = block.getProposer();
    Account proposerAccount = new AccountFixture(proposer);
    when(mockState.atBlockId(block.getPreviousBlockId())).thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(proposer)).thenReturn(proposerAccount);

    ///Verifier
    Validator verifier = new BlockVerifier(mockState);

    //Act
    boolean result = verifier.proposerHasEnoughStake(block);

    //Assert
    Assertions.assertTrue(result);
  }

  //
  // 12. allTransactionsValidated fails when there is at least one transaction that does not have a minimum number of
  //     certificates from staked validators that pass the signature verification.
  // 13. allTransactionsValidated passes when there all transactions have a minimum number of
  //     certificates from staked validators, and all certificates pass the signature verification.
  //
  // 14. allTransactionsSound fails when there is at least one transaction that fails on its soundness.
  // 15. allTransactionsSound passes when all transactions pass on their soundness.
  //
  // 16. noDuplicateSender fails when there is at least two distinct transactions in a block that share the same sender.
  @Test
  public void testBlockDuplicateSender() {
    //Arrange
    /// Block
    Identifier sender = IdentifierFixture.newIdentifier();
    ValidatedTransaction transaction1 = ValidatedTransactionFixture.newValidatedTransaction(sender);
    ValidatedTransaction transaction2 = ValidatedTransactionFixture.newValidatedTransaction(sender);
    ValidatedTransaction[] transactions = new ValidatedTransaction[2];
    transactions[0] = transaction1;
    transactions[1] = transaction2;
    Block block = BlockFixture.newBlock(transactions);

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    when(mockState.atBlockId(block.getPreviousBlockId())).thenReturn(mockSnapshot);

    ///Verifier
    Validator verifier = new BlockVerifier(mockState);

    //Act
    boolean result = verifier.noDuplicateSender(block);

    //Assert
    Assertions.assertFalse(result);
  }

  // 17. noDuplicateSender passes when all distinct transaction in a block have distinct senders.
  @Test
  public void testBlockNoDuplicateSender() {
    //Arrange
    /// Block
    Identifier sender = IdentifierFixture.newIdentifier();
    Identifier sender2 = IdentifierFixture.newIdentifier();
    ValidatedTransaction transaction1 = ValidatedTransactionFixture.newValidatedTransaction(sender);
    ValidatedTransaction transaction2 = ValidatedTransactionFixture.newValidatedTransaction(sender2);
    ValidatedTransaction[] transactions = new ValidatedTransaction[2];
    transactions[0] = transaction1;
    transactions[1] = transaction2;
    Block block = BlockFixture.newBlock(transactions);

    /// State & Snapshot Mocking
    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    when(mockState.atBlockId(block.getPreviousBlockId())).thenReturn(mockSnapshot);

    ///Verifier
    Validator verifier = new BlockVerifier(mockState);

    //Act
    boolean result = verifier.noDuplicateSender(block);

    //Assert
    Assertions.assertTrue(result);
  }
}
