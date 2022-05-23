package storage;

/**
 * Encapsulates tests for transactions database.
 */
public class TransactionsTest {
  // TODO: implement a unit test for each of the following scenarios:
  // IMPORTANT NOTE: each test must have a separate instance of database, and the database MUST only created on a
  // temporary directory.
  // In following tests by a "new" transaction, we mean transaction that already does not exist in the database,
  // and by a "duplicate" transaction, we mean one that already exists in the database.
  // 1. When adding 10 new transactions sequentially, the Add method must return true for all of them. Moreover, after
  //    adding transactions is done, querying the Has method for each of the transaction should return true. Also, when
  //    querying All method, list of all 10 transactions must be returned. Moreover, all transactions should be
  //    retrievable through get method.
  // 2. Repeat test case 1 for concurrently adding transactions as well as concurrently querying the database for has,
  //    and get.
  // 3. Add 10 new transactions, check that they are added correctly, i.e., while adding each transactions
  //    Add must return true, Has returns true for each of them, and All returns list of all of them, and get must
  //    return the transaction. Then Remove the first
  //    5 transactions. While Removing each of them, the Remove should return true.
  //    Then query all 10 transactions using Has. Has should return false for the first 5 transactions
  //    that have been removed, and get should return null for them. But for the last 5 transactions has
  //    should return true, and get should return the transaction. Also, All should return only the last 5 transactions.
  // 4. Repeat test case 3 for concurrently adding and removing transactions as well as concurrently querying the
  //    database for has and get.
  // 5. Add 10 new transactions and check that all of them are added correctly, i.e., while adding each transaction
  //    Add must return true, Has returns true for each of them, get should return the transaction,
  //    and All returns list of all of them. Then try Adding all of them again, and
  //    Add should return false for each of them,  while has should still return true, and get should be able to
  //    able to retrieve the transaction.
  // 6. Repeat test case 5 for concurrently adding transactions as well as concurrently querying the
  //    database for has, and get.
}
