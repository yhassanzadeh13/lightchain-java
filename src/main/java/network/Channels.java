package network;

/**
 * List of network channels.
 */
public class Channels {
  /**
   * Channel to disseminate proposed blocks.
   */
  // TODO: validator and proposer engines must register to this channel.
  public static final String ProposedBlocks = "proposed-blocks";

  /**
   * Channel to disseminate proposed transactions.
   */
  // TODO: validator engine must register to this channel.
  public static final String ProposedTransactions = "proposed-transactions";

  /**
   * Channel to disseminate validated blocks.
   */
  // TODO: proposer and ingest engines must register to this channel.
  public static final String ValidatedBlocks = "validated-blocks";

  /**
   * Channel to disseminate validated transactions.
   */
  // TODO: ingest engine must register to this channel.
  public static final String ValidatedTransactions = "validated-transactions";
}
