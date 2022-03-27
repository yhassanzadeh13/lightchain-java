package protocol.block;

import java.util.ArrayList;

import model.crypto.Signature;
import model.lightchain.Account;
import model.lightchain.Block;
import model.lightchain.Identifier;
import model.lightchain.ValidatedTransaction;
import protocol.Parameters;
import protocol.transaction.TransactionValidator;
import protocol.transaction.InfTransactionValidator;
import state.Snapshot;
import state.State;

/**
 * Represents a verifier class that is used to verify a block is valid.
 */
public class BlockValidator implements InfBlockValidator {
  /**
   * Unique State that the block refers to.
   */
  private final State state;

  /**
   * Constructor.
   */
  public BlockValidator(State state) {
    this.state = state;
  }

  /**
   * Validates block parameters.
   *
   * @param block the block under validation.
   * @return true if all block fields have a valid value, and false otherwise. A transaction is valid if the
   * previous block id is a valid and finalized block, the proposer refers to a valid identity at
   * snapshot of the previous block id, and the number of transactions are within the permissible range of LightChain
   * parameters.
   */
  @Override
  public boolean isCorrect(Block block) {
    Identifier previousBlockId = block.getPreviousBlockId();
    Snapshot snapshot = state.atBlockId(previousBlockId);
    if (snapshot == null) {
      // no valid snapshot exists for the parent block.
      return false;
    }
    Identifier proposer = block.getProposer();
    if (snapshot.getAccount(proposer) == null) {
      // proposer of this block is not a valid account.
      return false;
    }
    // for a block to be correct, its number of transactions must be within a permissible range.
    int transactions = block.getTransactions().length;
    return transactions >= Parameters.MIN_TRANSACTIONS_NUM && transactions <= Parameters.MAX_TRANSACTIONS_NUM;
  }

  /**
   * Validates the consistency of block based on LightChain protocol.
   *
   * @param block the block under validation.
   * @return true only if the previous block id this block refers to corresponds to the last snapshot on the
   * node's state. False otherwise.
   */
  @Override
  public boolean isConsistent(Block block) {
    return state.last()
        .getReferenceBlockId()
        .equals(block.getPreviousBlockId());
  }

  /**
   * Validates that the block is indeed issued by the proposer.
   *
   * @param block the block under validation.
   * @return true if the block has a valid signature that is verifiable by the public key of its proposer,
   * false otherwise.
   */
  @Override
  public boolean isAuthenticated(Block block) {
    return state.atBlockId(block.getPreviousBlockId())
        .getAccount(block.getProposer())
        .getPublicKey()
        .verifySignature(block, block.getSignature());
  }

  /**
   * Validates proposer has enough stake.
   *
   * @param block the block under validation.
   * @return true if proposer has a greater than or equal stake than the amount of the minimum required one based on
   * LightChain parameters, and false otherwise.
   * The stake of proposer must be checked at the snapshot of the reference block of the block.
   */
  @Override
  public boolean proposerHasEnoughStake(Block block) {
    return state.atBlockId(block.getPreviousBlockId())
        .getAccount(block.getProposer())
        .getStake() >= Parameters.MINIMUM_STAKE;
  }

  /**
   * Checks all transactions included in the block are validated.
   *
   * @param block the block under validation.
   * @return true if all transactions included in the block are validated, i.e., have a minimum of signature threshold
   * as specified by LightChain protocol. Signatures are verified based on the public key of validators at the snapshot
   * of the previous block id.
   * Also, all validators of each transaction has minimum stake at the previous block id of this block.
   */
  @Override
  public boolean allTransactionsValidated(Block block) {
    for (ValidatedTransaction transaction : block.getTransactions()) {
      if (transaction.getCertificates().length < Parameters.SIGNATURE_THRESHOLD) {
        // transaction has less that required number of validators.
        return false;
      }

      Snapshot snapshot = state.atBlockId(block.getPreviousBlockId());
      for (Signature signature : transaction.getCertificates()) {
        Account account = snapshot.getAccount(signature.getSignerId());
        if (account == null) {
          // signer of transactions is not a valid account
          return false;
        }
        if (account.getStake() < Parameters.MINIMUM_STAKE) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Checks all transactions included in the block are sound.
   *
   * @param block the block under validation.
   * @return true if all transactions included in the block are sound. Each individual transaction is sound
   * if its reference block id has a strictly higher height than the height of the
   * last block id in the sender account.
   */
  @Override
  public boolean allTransactionsSound(Block block) {
    InfTransactionValidator validator = new TransactionValidator(state);
    for (ValidatedTransaction transaction : block.getTransactions()) {
      if (!validator.isSound(transaction)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks no two validated transactions included in this block have the same sender.
   *
   * @param block the block under validation.
   * @return true if no two validated transactions included in this block have the same sender. False otherwise.
   */
  @Override
  public boolean noDuplicateSender(Block block) {
    ArrayList<Identifier> senders = new ArrayList<>();
    for (ValidatedTransaction transaction : block.getTransactions()) {
      if (senders.contains(transaction.getSender())) {
        return false;
      }
      senders.add(transaction.getSender());
    }
    return true;
  }
}
