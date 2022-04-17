package modules.ads;

import model.Entity;

/**
 * Models AuthenticatedDataStructure (ADS) and a key-value store of entities supported with membership proofs.
 */
public interface AuthenticatedDataStructure {
  AuthenticatedEntity put(Entity e);

  AuthenticatedEntity get(Entity e);
}
