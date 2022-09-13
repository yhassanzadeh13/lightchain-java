package storage.mapdb;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import model.lightchain.BlockProposal;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import storage.BlockProposals;

/**
 * Implementation of BlockProposals interface.
 */

public class BlockProposalsMapDb implements BlockProposals {
  /**
   lock variable to access read and write locks while accessing data.
   */
  private final ReentrantReadWriteLock lock;

  /**
   * db is used to refer and access to the database.
   */
  private final DB db;

  /**
   * name of the map created for block proposals.
   */
  private static final String BLOCK_PROPOSALS_MAP = "block_proposals_map";

  /**
   * key for last proposed block.
   */
  private static final String LAST_PROPOSED_BLOCK = "last_proposed_block";

  /**
   * runtime Hash tree map to storing Block proposals
   * with Strings as keys.
   */
  private final HTreeMap<String, BlockProposal> blockProposalsMap;
  public final String LAST_BLOCK_PROPOSAL_EXISTS = "Cannot overwrite existing "
      +
      "last block proposal. Clear last proposal before setting a new";

  /**
   * Creates a block proposals mapdb.
   *
   * @param filePath     of id, block proposal mapdb
   */
  public BlockProposalsMapDb(final String filePath) {
    this.db = DBMaker.fileDB(filePath).make();
    this.lock = new ReentrantReadWriteLock();
    this.blockProposalsMap =
      (HTreeMap<String, BlockProposal>)
        this.db.hashMap(BLOCK_PROPOSALS_MAP).createOrOpen();
  }

  /**
   * Sets the most recent proposal to the given one. At any time,
   * there is always one block proposal referring to the most recent one.
   * To call this method, first the clearLastProposal must be called
   * to make sure that the already existing last proposal is cleared.
   * Otherwise, it causes an IllegalStateException to set the last proposal
   * while another one already existing.
   *
   * @param proposal the proposal to be set as the latest one.
   * @throws IllegalStateException if a proposal already exists.
   */
  @Override
  public void setLastProposal(final BlockProposal proposal) throws IllegalStateException {
    try {
      lock.writeLock().lock();
      BlockProposal lastBlockProposal = this.blockProposalsMap.get(LAST_PROPOSED_BLOCK);
      if (lastBlockProposal != null) {
        throw new
          IllegalStateException(LAST_BLOCK_PROPOSAL_EXISTS);
      } else {
        this.blockProposalsMap.put(LAST_PROPOSED_BLOCK, proposal);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Returns the most recent proposal.
   *
   * @return returns the most recent proposal.
   */
  @Override
  public BlockProposal getLastProposal() {
    BlockProposal lastBlockProposal;
    try {
      lock.readLock().lock();
      lastBlockProposal = this.blockProposalsMap.get(LAST_PROPOSED_BLOCK);
    } finally {
      lock.readLock().unlock();
    }
    return lastBlockProposal;
  }

  /**
   * Clears the most recent proposal.
   */
  @Override
  public void clearLastProposal() {
    try {
      lock.writeLock().lock();
      this.blockProposalsMap.remove(LAST_PROPOSED_BLOCK);
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Close the db.
   */
  public void closeDb() {
    db.close();
  }
}

