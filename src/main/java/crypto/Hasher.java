package crypto;

import model.codec.EncodedEntity;
import model.crypto.Hash;

/**
 * Hasher generalizes the functionality of computing collision-resistant hash of EncodedEntities.
 */
public interface Hasher {
  /**
   * Computes hash of the given encoded entity.
   *
   * @param e input encoded entity.
   * @return hash object of the entity.
   */
  Hash computeHash(EncodedEntity e);
}
