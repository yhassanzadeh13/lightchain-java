package protocol.block;

/**
 * Encapsulates tests for block validation part of PoV consensus.
 */
public class ValidatorTest {
  // Note: except actual implementation of block Validator, mock everything else, and use fixtures when needed.
  //
  // TODO: a single individual test function for each of these scenarios:
  // 1. isCorrect fails since previous block id does not represent a valid snap shot (i.e., null snapshot).
  // 2. isCorrect fails when proposer does not refer to a valid account at the snapshot of previous block id.
  // 3. isCorrect fails when number of validated transactions included in the block are below min threshold.
  // 4. isCorrect fails when number of validated transactions included in the block are above max threshold
  // 5. isCorrect passes when all conditions satisfied.
  //
  // 6. isConsistent fails when previous block id does not refer to the latest snapshot of the validating node.
  // 7. isConsistent passes when previous block id refers to the latest snapshot of the validating node.
  //
  // 8. isAuthenticated fails since block signature verification against its proposer public key fails.
  // 9. isAuthenticated passes when block signature verification against its proposer public key passes.
  //
  // 10. proposerHashEnoughStake fails when proposer has a stake lower than minimum required stakes.
  // 11. proposerHasEnoughStake passes when proposer has enough amount of stake greater than or equal to
  //     minimum required one.
  //
  // 12. allTransactionsValidated fails when there is at least one transaction that does not have a minimum number of
  //     certificates from staked validators that pass the signature verification.
  // 13. allTransactionsValidated passes when there all transactions have a minimum number of
  //     certificates from staked validators, and all certificates pass the signature verification.
  //
  // 14. allTransactionsSound fails when there is at least one transaction that fails on its soundness.
  // 15. allTransactionsSound passes when all transactions pass on their soundness.
  //
  // 16. noDuplicateSender fails when there is at least two distinct transactions in a block that share the same sender.
  // 17. noDuplicateSender passes when all distinct transaction in a block have distinct senders.
}
