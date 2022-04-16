package networking.stub;

/**
 * Encapsulates tests for the storage side of the stub network.
 */
public class StubNetworkStorageTest {
  // TODO: implement test scenarios
  // Use mock engines with stub network.
  // 1. Engine A1 (on one network) puts an entity on channel1 and Engine B1 on another network can get it on the
  //    same channel1 successfully, while Engine B2 on another channel2 can't get it successfully.
  // 2. Engine A1 (on one network)  can CONCURRENTLY put 100 different entities on channel1, and
  //    Engine B1 on another network can get each entity using its entity id only the the same channel,
  //    while Engine B2 on another channel2 can't get it any of them successfully.
  // 3. Engine A1 (on one network)  can CONCURRENTLY put 100 different entities on channel1, and
  //    Engine B1 on another network can get all of them at once using allEntities method,
  //    while Engine B2 on another channel2 can't get none of them using all.
}
