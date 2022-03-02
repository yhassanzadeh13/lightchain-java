package protocol;

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
   * Maximum number of validators a node can seek for a block or transaction.
   */
  public static final int VALIDATOR_THRESHOLD = 10;
  /**
   * Maximum number of validators' signature a block or transaction must have to be considered as validated. 
   */
  public static final int SIGNATURE_THRESHOLD = 10;
}
