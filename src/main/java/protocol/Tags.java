package protocol;

/**
 * Encapsulates list of tags used in hashing an entity.
 */
public class Tags {
  /**
   * ValidatorTag is used in the validator assigner to determine validators of a block or transaction.
   */
  public static final String ValidatorTag = "validator-tag";

  /**
   * BlockProposerTag is used in the validator assigner to determine proposer of a new block.
   */
  public static final String BlockProposerTag = "block-proposer-tag";
}
