package protocol.transaction;

public class ValidatorTest {
  // Note: except actual implementation of Validator, mock everything else, and use fixtures when needed. 
  //
  // TODO: a single individual test function for each of these scenarios:
  // 1. isCorrect fails since reference block id does not represent a valid snap shot (i.e., null snapshot).
  // 2. isCorrect fails since sender does not refer to a valid account at the snapshot of reference block.
  // 3. isCorrect fails since receiver does not refer to a valid account at the snapshot of reference block.
  // 4. isCorrect fails since amount is negative (and also a case for zero).
  // 5. isCorrect passes when all conditions satisfied.
  //
  // 6. isSound fails since reference block has a lower height (and also case for equal) than the last block of sender account.
  // 7. isSound passes since reference block has a higher height than the last block of the sender account.
  //
  // 8. isAuthenticated fails since transaction signature verification against its sender public key fails.
  // 9. isAuthenticated passes when transaction signature verification against its sender public key passes.
  //
  // 10. senderHashEnoughBalance fails when sender has a balance lower than transaction amount.
  // 11. senderHashEnoughBalance passes when sender has a balance greater than or equal to the transaction amount.
}
