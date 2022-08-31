package unittest.fixtures.networking.p2p;

/**
 * Encapsulates tests for p2p storage.
 */
public class StorageTest {
  // Add a test case for each of following scenarios, for now use a hash table per network instance for storage of
  // entities.
  // 1.Run a system with 10 networks each with one engine on channel 1 and one engine on channel 2.
  // Each engine concurrently puts 100 entities on the channel that it is registered on.
  // Then, all engines registering on channel 1 should be able to get all entities that other
  // engines have put on this channel (total of 1000), while could not get any of
  // entities that have been put on channel 2. The same should be true for engines of channel 2.
  //
  // 2. Create 10 networks, all having two mock engine registering one registering on
  // channel 1 and the other on channel 2. Each engine concurrently puts 100 entities on the channel
  // that it is registered on. Then check the Distributed storage component of
  // each stub network, and all stored entities on the Distributed storage components of each node
  // should satisfy condition 2 regarding their identifier. Moreover, the union of all stored entities
  // across all storage components should be exactly the 1000 original entities that are stored.
  // Moreover, no two Distributed storage components should share the same entity.
  //
  // 3. Create 10 networks, all having two mock engine registering one registering
  // on channel 1 and the other on channel 2.
  // Each engine concurrently puts only one entity 100 times on the channel it is registered on.
  // So a total of 10 unique entities are stored.
  // Then check the union of all stored entities across all storage components
  // should be exactly the 10 unique original entities that are stored.
}
