package protocol.assigner;

/**
 * Encapsulates tests for validator assignment.
 */
public class AssignerTest {
  // TODO: develop a test covering each of the following scenario.
  // For all tests consider a snapshot of 10 staked accounts and
  // 10 unstaked accounts as well as a mock identifier (for assigner parameter).
  // then run assigner with following num parameters.
  // 1. num = 1 should pick one of the staked accounts, also, running the test 100 times always picks the same account.
  // 2. num = 2 should pick two of the staked accounts, also, running the test 100 times always picks the same accounts.
  // 2. num = 5 should pick five of the staked accounts,
  //    also, running the test 100 times always picks the same accounts.
  // 3. num = 10 should pick all 10 staked accounts, also, running the test 100 times always picks the same accounts.
  // 4. num = 11 should return an IllegalArgumentException.
  // 5. Running case 1 with an empty snapshot must return an IllegalArgumentException.
  // 6. If any of the given parameters are null, IllegalStateArgument returned.
  // 7. num = 0 returns an empty (but not null) assignment.
}
