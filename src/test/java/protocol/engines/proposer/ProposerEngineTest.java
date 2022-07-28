package protocol.engines.proposer;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import model.Entity;
import model.exceptions.LightChainNetworkingException;
import model.lightchain.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import protocol.Parameters;
import protocol.block.BlockValidator;
import storage.Blocks;
import unittest.fixtures.*;

/**
 * Encapsulates tests for the proposer engine.
 */
public class ProposerEngineTest {

  /**
   * Evaluates happy path of ProposerEngine, i.e., proposing a new block to the validators when it is assigned as the proposer.
   */
  @Test
  public void happyPath() {
    Block currentBlock = BlockFixture.newBlock(Parameters.MIN_TRANSACTIONS_NUM + 1);

    ProposerParameterFixture params = new ProposerParameterFixture();
    params.mockBlocksStorageForBlock(currentBlock);
    // mocks this node as the proposer of the next block.
    params.mockIdAsNextBlockProposer(currentBlock);
    // mocks enough validated pending transactions.
    params.mockValidatedTransactions(1);
    // mocks validators
    ArrayList<Identifier> validators = IdentifierFixture.newIdentifiers(Parameters.VALIDATOR_THRESHOLD);
    params.mockValidatorAssigner(validators);

    // mocks networking layer and conduit registration.
    CountDownLatch blockSentForValidation = new CountDownLatch(Parameters.VALIDATOR_THRESHOLD);
    try {
      doAnswer(invocationOnMock -> {
        Block block = invocationOnMock.getArgument(0, Block.class);
        // checks whether block is correct and authenticated.
        BlockValidator validator = new BlockValidator(params.state);
        Assertions.assertTrue(validator.isCorrect(block));
        Assertions.assertTrue(validator.isAuthenticated(block));

        // block should be sent to an assigned validator for validation.
        Identifier validatorId = invocationOnMock.getArgument(1, Identifier.class);
        Assertions.assertTrue(validators.contains(validatorId));

        blockSentForValidation.countDown();
        return null;
      }).when(params.proposedConduit).unicast(any(Block.class), any(Identifier.class));
    } catch (LightChainNetworkingException e) {
      Assertions.fail();
    }


    ProposerEngine proposerEngine = new ProposerEngine(params);
    proposerEngine.onNewValidatedBlock(currentBlock.getHeight(), currentBlock.id());

    try {
      boolean doneOnTime = blockSentForValidation.await(1000, TimeUnit.MILLISECONDS);
      Assertions.assertTrue(doneOnTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
  }

  /**
   * Evaluates that when a new block is received while it is pending
   * for its previously proposed block to get validated, the engine throws an IllegalStateException.
   */
  @Test
  public void newValidatedBlockWhilePendingValidation() {
    Block block = BlockFixture.newBlock(Parameters.MIN_TRANSACTIONS_NUM + 1);

    ProposerParameterFixture params = new ProposerParameterFixture();
    params.mockBlocksStorageForBlock(block);
    // mocks an existing proposed block.
    when(params.blocks.byTag(Blocks.TAG_LAST_PROPOSED_BLOCK)).thenReturn(BlockFixture.newBlock());

    ProposerEngine engine = new ProposerEngine(params);
    Assertions.assertThrows(IllegalStateException.class, () -> {
      engine.onNewValidatedBlock(block.getHeight(), block.id());
    });
  }

  /**
   * Evaluates that when a new block is received but the block cannot be found on its storage.
   * It should throw an IllegalArgumentException.
   */
  @Test
  public void blockNotInDatabase() {
    Block block = BlockFixture.newBlock(Parameters.MIN_TRANSACTIONS_NUM + 1);

    ProposerParameterFixture params = new ProposerParameterFixture();
    // mocks an existing proposed block.
    when(params.blocks.has(block.id())).thenReturn(false);

    ProposerEngine proposerEngine = new ProposerEngine(params);
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      proposerEngine.onNewValidatedBlock(block.getHeight(), block.id());
    });
  }

  /**
   * Evaluates when unicasting next proposed block to any of the validators fails. The proposed block should never male persistent.
   */
  @Test
  public void failedUnicast() throws LightChainNetworkingException {
    Block currentBlock = BlockFixture.newBlock(Parameters.MIN_TRANSACTIONS_NUM + 1);

    ProposerParameterFixture params = new ProposerParameterFixture();
    params.mockBlocksStorageForBlock(currentBlock);
    // mocks this node as the proposer of the next block.
    params.mockIdAsNextBlockProposer(currentBlock);
    // mocks enough validated pending transactions.
    params.mockValidatedTransactions(1);
    // mocks validators
    ArrayList<Identifier> validators = IdentifierFixture.newIdentifiers(Parameters.VALIDATOR_THRESHOLD);
    params.mockValidatorAssigner(validators);

    // mocks unicasting faces an exception
    doThrow(new LightChainNetworkingException("exception", new Throwable())).when(params.proposedConduit)
        .unicast(any(Entity.class), any(Identifier.class));


    ProposerEngine proposerEngine = new ProposerEngine(params);

    Assertions.assertThrows(IllegalStateException.class, () -> {
      proposerEngine.onNewValidatedBlock(currentBlock.getHeight(), currentBlock.id());
    });

    // proposed block should never make persistent
    verify(params.blocks, never()).writeTag(eq(Blocks.TAG_LAST_PROPOSED_BLOCK), any(Block.class));
  }

  /**
   * Evaluates that when enough block approvals are received,
   * a validated block is created and sent to the network.
   */
  @Test
  public void enoughBlockApproval() throws LightChainNetworkingException {
    ProposerParameterFixture params = new ProposerParameterFixture();
    Block proposedBlock = new Block(
        IdentifierFixture.newIdentifier(),
        params.local.myId(),
        100,
        ValidatedTransactionFixture.newValidatedTransactions(10));
    params.mockProposedBlock(proposedBlock);
    ArrayList<Account> accounts = AccountFixture.newAccounts(10);
    params.mockSnapshotAtBlock(accounts, proposedBlock.getPreviousBlockId());


    // Verification.
    ProposerEngine proposerEngine = new ProposerEngine(params);

    try {
      doAnswer(invocationOnMock -> {
        ValidatedBlock block = invocationOnMock.getArgument(0, ValidatedBlock.class);

        // checks whether block is correct and authenticated.
        Assertions.assertTrue(params.local.myPublicKey().verifySignature(proposedBlock, block.getSignature()));
        // checks all fields of validated block matches with proposed block
        // TODO: also check for certificaties.
        Assertions.assertEquals(proposedBlock.getPreviousBlockId(), block.getPreviousBlockId());
        Assertions.assertEquals(proposedBlock.getProposer(), params.local.myId());
        Assertions.assertArrayEquals(proposedBlock.getTransactions(), block.getTransactions());
        Assertions.assertEquals(proposedBlock.getHeight(), block.getHeight());

        return null;
      }).when(params.validatedConduit).unicast(any(ValidatedBlock.class), any(Identifier.class));
    } catch (LightChainNetworkingException e) {
      Assertions.fail();
    }

    for (int i = 0; i < Parameters.VALIDATOR_THRESHOLD; i++) {
      BlockApproval blockApproval = new BlockApproval(SignatureFixture.newSignatureFixture(), proposedBlock.id());
      proposerEngine.process(blockApproval);
    }

    // validated block must be sent to all nodes.
    verify(params.validatedConduit, times(10)).unicast(any(ValidatedBlock.class), any(Identifier.class));
  }

  /**
   * Evaluates that when enough block approvals are received concurrently,
   * a validated block is created and sent to the network (including itself).
   */
  @Test
  public void enoughBlockApprovalConcurrently() throws LightChainNetworkingException, InterruptedException {
    ProposerParameterFixture params = new ProposerParameterFixture();
    Block proposedBlock = new Block(
        IdentifierFixture.newIdentifier(),
        params.local.myId(),
        100,
        ValidatedTransactionFixture.newValidatedTransactions(10));
    params.mockProposedBlock(proposedBlock);
    ArrayList<Account> accounts = AccountFixture.newAccounts(10);
    params.mockSnapshotAtBlock(accounts, proposedBlock.getPreviousBlockId());


    // Verification.
    ProposerEngine proposerEngine = new ProposerEngine(params);

    try {
      doAnswer(invocationOnMock -> {
        ValidatedBlock block = invocationOnMock.getArgument(0, ValidatedBlock.class);

        // checks whether block is correct and authenticated.
        Assertions.assertTrue(params.local.myPublicKey().verifySignature(proposedBlock, block.getSignature()));
        // checks all fields of validated block matches with proposed block
        // TODO: also check for certificaties.
        Assertions.assertEquals(proposedBlock.getPreviousBlockId(), block.getPreviousBlockId());
        Assertions.assertEquals(proposedBlock.getProposer(), params.local.myId());
        Assertions.assertArrayEquals(proposedBlock.getTransactions(), block.getTransactions());
        Assertions.assertEquals(proposedBlock.getHeight(), block.getHeight());

        return null;
      }).when(params.validatedConduit).unicast(any(ValidatedBlock.class), any(Identifier.class));
    } catch (LightChainNetworkingException e) {
      Assertions.fail();
    }

    CountDownLatch processApprovalCd = new CountDownLatch(10);
    for (int i = 0; i < Parameters.VALIDATOR_THRESHOLD; i++) {
      new Thread(() -> {
        final BlockApproval blockApproval = new BlockApproval(SignatureFixture.newSignatureFixture(), proposedBlock.id());
        proposerEngine.process(blockApproval);
        processApprovalCd.countDown();
      }).start();
    }

    Assertions.assertTrue(processApprovalCd.await(10, TimeUnit.SECONDS));
    // validated block must be sent to all nodes.
    verify(params.validatedConduit, times(10)).unicast(any(ValidatedBlock.class), any(Identifier.class));
  }

}