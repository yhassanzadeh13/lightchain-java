package protocol.block;

import java.util.ArrayList;

import model.crypto.Signature;
import model.lightchain.Block;
import model.lightchain.Identifier;
import model.lightchain.ValidatedTransaction;
import protocol.Parameters;
import protocol.transaction.TransactionVerifier;
import state.Snapshot;
import state.State;


/**
 * Represents a verifier class that is used to verify a block is valid.
 */
public class BlockVerifier implements Validator {
  /**
   * Unique State that the block is in.
   */
  private final State state;

  /**
   * Constructor of a BlockVerifier.
   */
  public BlockVerifier(State state) {
    this.state = state;
  }

  /**
   * Validates block parameters are all correct.
   */
  @Override
  public boolean isCorrect(Block block) {
    Identifier previousBlockId = block.getPreviousBlockId();
    Snapshot snapshot = state.atBlockId(previousBlockId);
    if (snapshot == null) {
      return false;
    }
    Identifier proposer = block.getProposer();
    if (snapshot.getAccount(proposer) == null) {
      return false;
    }
    int transactions = block.getTransactions().length;

    return transactions >= Parameters.MIN_TRANSACTIONS_NUM && transactions <= Parameters.MAX_TRANSACTIONS_NUM;
  }

  /**
   * Validates the consistency of block based on LightChain protocol.
   */
  @Override
  public boolean isConsistent(Block block) {
    return state.last().equals(state.atBlockId(block.getPreviousBlockId()));
  }

  /**
   * Validates that the block is indeed issued by the proposer.
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
        return false;
      }
      for (Signature signature : transaction.getCertificates()) {
        if (state.atBlockId(block.getPreviousBlockId())
            .getAccount(signature.getSignerId())
            .getStake() < Parameters.MINIMUM_STAKE) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Checks all transactions included in the block are sound.
   */
  @Override
  public boolean allTransactionsSound(Block block) {
    TransactionVerifier verifier = new TransactionVerifier(state);
    for (ValidatedTransaction transaction : block.getTransactions()) {
      if (!verifier.isSound(transaction)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks no two validated transactions included in this block have the same sender.
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
