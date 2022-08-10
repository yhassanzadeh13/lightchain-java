package protocol.block;

import java.util.ArrayList;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import model.crypto.PublicKey;
import model.crypto.Signature;
import model.lightchain.*;
import protocol.Parameters;
import protocol.transaction.InfTransactionValidator;
import protocol.transaction.TransactionValidator;
import state.Snapshot;
import state.State;

/**
 * Represents a verifier class that is used to verify a block is valid.
 */
public class BlockValidator implements InfBlockValidator {
  /**
   * Unique State that the block refers to.
   */
  private State state;

  /**
   * Constructor.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "we want state being intentionally mutable externally")
  public BlockValidator(State state) {
    this.state = state;
  }

  /**
   * Validates block proposal parameters.
   *
   * @param proposal the block under validation.
   * @return true if all proposal fields have a valid value, and false otherwise. A proposal is valid if the
   * previous block id is a valid and finalized block, the proposer refers to a valid identity at
   * snapshot of the previous block id, and the number of transactions are within the permissible range of LightChain
   * parameters.
   */
  @Override
  public boolean isCorrect(BlockProposal proposal) {
    Identifier previousBlockId = proposal.getPreviousBlockId();
    Snapshot snapshot = state.atBlockId(previousBlockId);
    if (snapshot == null) {
      // no valid snapshot exists for the parent block.
      return false;
    }
    Identifier proposer = proposal.getProposerId();
    if (snapshot.getAccount(proposer) == null) {
      // proposer of this block is not a valid account.
      return false;
    }
    // for a block to be correct, its number of transactions must be within a permissible range.
    int transactions = proposal.getTransactions().length;
    return transactions >= Parameters.MIN_TRANSACTIONS_NUM && transactions <= Parameters.MAX_TRANSACTIONS_NUM;
  }

  /**
   * Validates the consistency of block proposal based on LightChain protocol.
   *
   * @param proposal the block proposal under validation.
   * @return true only if the previous block id this block proposal refers to corresponds to the last snapshot on the
   * node's state. False otherwise.
   */
  @Override
  public boolean isConsistent(BlockProposal proposal) {
    return state.last()
        .getReferenceBlockId()
        .equals(proposal.getPreviousBlockId());
  }

  /**
   * Validates that the block proposal is indeed issued by the proposer.
   *
   * @param proposal the block proposal under validation.
   * @return true if the block proposal has a valid signature that is verifiable by the public key of its proposer,
   * false otherwise.
   */
  @Override
  public boolean isAuthenticated(BlockProposal proposal) {
    Snapshot snapshot = state.atBlockId(proposal.getPreviousBlockId());
    Account account = snapshot.getAccount(proposal.getProposerId());
    PublicKey publicKey = account.getPublicKey();
    return publicKey.verifySignature(
        // Note: casting block into a new block that includes all fields EXCEPT signature, hence the block identifier
        // is correctly computed as the identifier at the signature time.
        proposal.getHeader(),
        proposal.getSignature());
  }

  /**
   * Validates proposer has enough stake.
   *
   * @param proposal the block proposal under validation.
   * @return true if proposer has a greater than or equal stake than the amount of the minimum required one based on
   * LightChain parameters, and false otherwise.
   * The stake of proposer must be checked at the snapshot of the reference block of the block proposal.
   */
  @Override
  public boolean proposerHasEnoughStake(BlockProposal proposal) {
    return state.atBlockId(proposal.getPreviousBlockId())
        .getAccount(proposal.getProposerId())
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
