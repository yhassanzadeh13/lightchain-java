package protocol.engines.proposer;

import model.local.Local;
import network.Network;
import protocol.assigner.ProposerAssigner;
import protocol.assigner.ValidatorAssigner;
import state.State;
import storage.BlockProposals;
import storage.Blocks;
import storage.Transactions;

/**
 * Encapsulates the initialization parameters of ProposerEngine.
 */
public class ProposerParameters {
  public Blocks blocks;
  public Transactions pendingTransactions;
  public State state;
  public Local local;
  public Network network;
  public ValidatorAssigner validatorAssigner;
  public ProposerAssigner proposerAssigner;
  public BlockProposals blockProposals;

  public ProposerParameters() {
  }
}

