package storage;

import model.lightchain.BlockProposal;

/**
 * Persistent storage interface for maintaining block proposals.
 */
public interface BlockProposals {
  /**
   * Sets the most recent proposal to the given one. At any time, there is always one block proposal referring to the most recent one.
   * To call this method, first the clearLastProposal must be called to make sure that the already existing last proposal is cleared.
   * Otherwise, it causes an IllegalStateException to set the last proposal while another one already existing.
   *
   * @param proposal the proposal to be set as the latest one.
   * @throws IllegalStateException if a proposal already exists.
   */
  void setLastProposal(BlockProposal proposal) throws IllegalStateException;

  /**
   * Returns the most recent proposal.
   *
   * @return returns the most recent proposal.
   */
  BlockProposal getLastProposal();

  /**
   * Clears the most recent proposal.
   */
  void clearLastProposal();
}
