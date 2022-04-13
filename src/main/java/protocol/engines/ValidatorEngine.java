package protocol.engines;

import model.Entity;
import model.codec.EntityType;
import model.crypto.Signature;
import model.exceptions.LightChainNetworkingException;
import model.lightchain.Assignment;
import model.lightchain.Block;
import model.lightchain.Identifier;
import model.lightchain.Transaction;
import model.local.Local;
import network.Conduit;
import network.Network;
import protocol.Engine;
import protocol.Parameters;
import protocol.assigner.LightChainValidatorAssigner;
import protocol.block.BlockValidator;
import protocol.transaction.TransactionValidator;
import state.State;

import java.util.concurrent.locks.ReentrantLock;

/**
 * ValidatorEngine is a standalone engine of LightChain that runs transaction and block validation.
 */
public class ValidatorEngine implements Engine {
  private final Local local;
  private final Conduit con;
  private final State state;

  public ValidatorEngine(Network net, Local local, State state) {
    this.local = local;
    this.con = net.register(this, "validator");
    this.state = state;
  }

  private static final ReentrantLock lock = new ReentrantLock();

  /**
   * Received entity to this engine can be either a block or a transaction, anything else should throw an exception.
   * Upon receiving a block or transaction, the engine runs the assignment and checks whether the current node
   * is an assigned validator for that transaction or block. If the current node is not assigned, the engine
   * discards the entity.
   * If received entity is a block, the engine runs block validation on it, and if it passes the validation,
   * the engine signs the identifier of that block and sends the signature to the proposer of the block.
   * If received entity is a transaction, it runs the transaction validation on it.
   * If the transaction passes validation,
   * the engine signs the identifier of that transaction and sends the signature to the sender of that transaction.
   *
   * @param e the arrived Entity from the network, it should be either a transaction or a block.
   * @throws IllegalArgumentException when the arrived entity is neither a transaction nor a block.
   */
  @Override
  public void process(Entity e) throws IllegalArgumentException {

    if (e.type().equals(EntityType.TYPE_VALIDATED_BLOCK) || e.type().equals(EntityType.TYPE_VALIDATED_TRANSACTION)) {
      lock.lock();
      LightChainValidatorAssigner assigner = new LightChainValidatorAssigner();
      Identifier currentNode = this.local.myId();

      if (e.type().equals(EntityType.TYPE_BLOCK)) {
        Assignment assignment = assigner.assign(e.id(), state.atBlockId(((Block) e).getPreviousBlockId()),
            (short) Parameters.VALIDATOR_THRESHOLD);
        if (isBlockValidated((Block) e) && assignment.has(currentNode)) {
          Block b = (Block) e;
          Signature sign = this.local.signEntity(b);
          try {
            this.con.unicast(sign, (b.getProposer()));
          } catch (LightChainNetworkingException ex) {
            lock.unlock();
            ex.printStackTrace();
          }
        }
      } else if (e.type().equals(EntityType.TYPE_TRANSACTION)) {
        Assignment assignment = assigner.assign(e.id(), state.atBlockId(((Transaction) e).getReferenceBlockId()),
            (short) Parameters.VALIDATOR_THRESHOLD);
        if (isTransactionValidated((Transaction) e) && assignment.has(currentNode)) {
          Transaction tx = (Transaction) e;
          Signature sign = this.local.signEntity(tx);
          try {
            this.con.unicast(sign, (tx.getSender()));
          } catch (LightChainNetworkingException ex) {
            lock.unlock();
            ex.printStackTrace();
          }
        }
      }
      lock.unlock();
    } else {
      throw new IllegalArgumentException("entity is neither a block nor a transaction");
    }
  }

  private boolean isBlockValidated(Block b) {
    BlockValidator verifier = new BlockValidator(state);
    return verifier.allTransactionsSound(b) && verifier.allTransactionsValidated(b) && verifier.isAuthenticated(b)
        && verifier.isConsistent(b) && verifier.isCorrect(b) && verifier.noDuplicateSender(b) && verifier.proposerHasEnoughStake(b);
  }

  private boolean isTransactionValidated(Transaction t) {
    TransactionValidator verifier = new TransactionValidator(state);
    return verifier.isSound(t) && verifier.senderHasEnoughBalance(t) && verifier.isAuthenticated(t) && verifier.isCorrect(t);
  }
}
