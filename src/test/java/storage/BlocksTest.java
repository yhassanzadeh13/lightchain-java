package storage;

/**
 * Encapsulates tests for block database. 
 */
public class BlocksTest {
  // TODO: implement a unit test for each of the following scenarios:
  // IMPORTANT NOTE: each test must have a separate instance of database, and the database MUST only created on a
  // temporary directory.
  // In following tests by a "new" block, we mean a block that already does not exist in the database,
  // and by a "duplicate" block, we mean one that already exists in the database.
  // 1. When adding 10 new blocks sequentially, the Add method must return true for all of them. Moreover, after
  //    adding blocks is done, querying the Has method for each of the block should return true. After adding all blocks
  //    are done, each block must be retrievable using both its id (byId) as well as its height (byHeight). Also, when
  //    querying All method, list of all 10 block must be returned.
  // 2. Repeat test case 1 for concurrently adding blocks as well as concurrently querying the database for has, byId,
  //    and byHeight.
  // 3. Add 10 new blocks sequentially, check that they are added correctly, i.e., while adding each block Add must return
  //    true, Has returns true for each of them, each block is retrievable by both its height and its identifier,
  //    and All returns list of all of them. Then Remove the first 5 blocks sequentially.
  //    While Removing each of them, the Remove should return true. Then query all 10 blocks using has, byId,
  //    and byHeight.
  //    Has should return false for the first 5 blocks have been removed,
  //    and byId and byHeight should return null. But for the last 5 blocks, has should return true, and byId
  //    and byHeight should successfully retrieve the exact block. Also, All should return only the last 5 blocks.
  // 4. Repeat test case 3 for concurrently adding and removing blocks as well as concurrently querying the
  //    database for has, byId, and byHeight.
  // 5. Add 10 new blocks and check that all of them are added correctly, i.e., while adding each block
  //    Add must return true, has returns true for each of them, and All returns list of all of them. Moreover, each
  //    block is retrievable using its identifier (byId) and height (byHeight). Then try Adding all of them again, and
  //    Add should return false for each of them, while has should still return true, and byId and byHeight should be
  //    able to retrieve the block.
  // 6. Repeat test case 5 for concurrently adding blocks as well as concurrently querying the
  //    database for has, byId, and byHeight.
}
