package storage;

/**
 * Encapsulates tests for identifiers database.
 */
public class IdentifiersTest {
  // TODO: implement a unit test for each of the following scenarios:
  // IMPORTANT NOTE: each test must have a separate instance of database, and the database MUST only created on a
  // temporary directory.
  // In following tests by a "new" identifier, we mean an identifier that already does not exist in the database,
  // and by a "duplicate" identifier, we mean one that already exists in the database.
  // 1. When adding 10 new identifiers sequentially, the Add method must return true for all of them. Moreover, after
  //    adding identifiers is done, querying the Has method for each of the identifiers should return true. Also, when
  //    querying All method, list of all 10 identifiers must be returned.
  // 2. Add 10 new identifiers, check that they are added correctly, i.e., while adding each identifier Add must return
  //    true, Has returns true for each of them, and All returns list of all of them. Then Remove the first
  //    5 identifiers.
  //    While Removing each of them, the Remove should return true. Then query all 10 identifiers using Has.
  //    Has should return false for the first 5 identifiers that have been removed. But for the last 5 identifiers it
  //    should return true. Also, All should return only the last 5 identifiers.
  // 3. Add 10 new identifiers and check that all of them are added correctly, i.e., while adding each identifier
  //    Add must return true,
  //    Has returns true for each of them, and All returns list of all of them. Then try Adding all of them again, and
  //    Add should return false for each of them.
}
