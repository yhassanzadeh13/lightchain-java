package protocol;

/**
 * Encapsulated LightChain's operational parameters.
 */
public class Parameters {
  /**
   * Maximum number of validated transactions a block can carry on.
   */
  public static final int MAX_TRANSACTIONS_NUM = 10;
  /**
   * Minimum number of validated transactions a block can carry on.
   */
  public static final int MIN_TRANSACTIONS_NUM = 1;
  /**
   * Minimum number of validated transactions a block should carry on.
   */
  public static final int MIN_VALIDATED_TRANSACTIONS_NUM = 1;
  /**
   * Maximum number of validators a node can seek for a block or transaction.
   */
  public static final short VALIDATOR_THRESHOLD = 10;
  /**
   * Maximum number of validators' signature a block or transaction must have to be considered as validated.
   */
  public static final int SIGNATURE_THRESHOLD = 10;
  /**
   * Minimum stake a node needed in terms of LightChain tokens in order to be eligible to propose a block, as well
   * as to validate a transaction or block.
   */
  public static final int MINIMUM_STAKE = 10;
}