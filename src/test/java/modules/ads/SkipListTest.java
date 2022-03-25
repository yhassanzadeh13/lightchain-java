package modules.ads;

/**
 * Encapsulates tests for an authenticated and concurrent implementation of SkipList ADS.
 */
public class SkipListTest {
  // TODO: writing tests to cover
  // 1. When putting a unique entity into skip list, we can recover it.
  // 2. Proof of membership for putting and getting an entity is the same.
  // 3. Putting an already existing entity does not change its membership proof.
  // 4. Putting 100 distinct entities concurrently inserts all of them into skip list with correct membership proofs,
  //  and also, makes them all retrievable with correct membership proofs.
  // 5. Getting non-existing identifiers returns null.
  // 7. Putting null returns null.
  // 8. Tampering with root identifier of an authenticated entity fails its verification.
  // 9. Tampering with entity of an authenticated entity fails its verification.
  // 10. Tampering with proof of an authenticated entity fails its verification.
}
