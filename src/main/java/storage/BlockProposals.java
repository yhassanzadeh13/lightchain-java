package storage;

import model.lightchain.BlockProposal;

public interface BlockProposals {
  void SetLastProposal(BlockProposal proposal) throws IllegalStateException;

  BlockProposal GetLastProposal();

  void ClearLastProposal();
}
