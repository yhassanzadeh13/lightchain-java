package modules.ads;

import model.Entity;
import model.lightchain.Identifier;

/**
 * Models AuthenticatedDataStructure (ADS) and a key-value store of entities supported with membership proofs.
 */
public interface AuthenticatedDataStructure {

  /**
   * Puts the given entity into the merkle tree and return AuthenticationEntity.
   *
   * @param e the entity to be put into the merkle tree
   *
   * @return AuthenticationEntity of the given entity
   */
  AuthenticatedEntity put(Entity e);

  /**
   * Return the AuthenticationEntity of the given identifier.
   *
   * @param id the identifier whose AuthenticationEntity is to be returned
   *
   * @return the AuthenticationEntity of the given identifier
   */
  AuthenticatedEntity get(Identifier id);

}
