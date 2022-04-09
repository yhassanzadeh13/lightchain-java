package storage;

/**
 * Encapsulates tests for distributed storage.
 */
public class DistributedTest {
  // TODO: implement a unit test for each of the following scenarios:
  // IMPORTANT NOTE: each test must have a separate instance of database, and the database MUST only created on a
  // temporary directory.
  // In following tests by a "new" entity, we mean an entity that already does not exist in the database,
  // and by a "duplicate" entity, we mean one that already exists in the database.
  // 1. When adding 20 new entities of different types (10 transactions and 10 blocks) sequentially,
  //    the Add method must return true for all of them. Moreover, after
  //    adding entities are done, querying the Has method for each of the entities should return true.
  //    After adding all entities
  //    are done, each entity must be retrievable using both its id (get). Also, when
  //    querying All method, list of all 20 entities must be returned.
  // 2. Repeat test case 1 for concurrently adding entities as well as concurrently querying the database for has, and
  //    get.
  // 3. Add 20 new entities sequentially (10 transactions and 10 blocks), check that they are added correctly, i.e.,
  //    while adding each entity Add must return
  //    true, Has returns true for each of them, each entity is retrievable by its identifier,
  //    and All returns list of all of them.
  //    Then Remove the first 10 entities (5 blocks and 5 transactions) sequentially.
  //    While Removing each of them, the Remove should return true. Then query all 20 entities using has, and get.
  //    Has should return false for the first 5 blocks amd 5 transactions that have been removed,
  //    and get should return null. But for the last 5 blocks and 5 transactions, has should return true, and get
  //    should successfully retrieve the exact entity.
  //    Also, All should return only the last 5 blocks and 5 transactions.
  // 4. Repeat test case 3 for concurrently adding and removing entities as well as concurrently querying the
  //    database for has, and get.
  // 5. Add 20 new entities (10 blocks, and 10 transactions)
  //    and check that all of them are added correctly, i.e., while adding each entity
  //    Add must return true, has returns true for each of them, and All returns list of all of them. Moreover, each
  //    entity is retrievable using its identifier (get). Then try Adding all of them again, and
  //    Add should return false for each of them, while has should still return true, and get should be
  //    able to retrieve the entity.
  // 6. Repeat test case 5 for concurrently adding entities as well as concurrently querying the
  //    database for has, get. 
}
