package protocol.engines.proposer;

import model.local.Local;
import network.Network;
import protocol.assigner.ProposerAssignerInf;
import protocol.assigner.ValidatorAssignerInf;
import state.State;
import storage.Blocks;
import storage.Transactions;

public class ProposerParameters {
  public Blocks blocks;
  public Transactions pendingTransactions;
  public State state;
  public Local local;
  public Network network;
  public ValidatorAssignerInf validatorAssigner;
  public ProposerAssignerInf proposerAssigner;

  public ProposerParameters() {
  }
}

