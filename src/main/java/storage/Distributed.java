package storage;

import java.util.ArrayList;

import model.Entity;
import model.exceptions.CodecException;
import model.lightchain.Identifier;

/**
 * Models distributed component of storage for a lightchain node that keeps entities in general form.
 * Note that under the hood, an entity is encoded and kept as an EncodedEntity.
 */
public interface Distributed {
  /**
   * Checks existence of entity on the database.
   *
   * @param entityId Identifier of entity.
   * @return true if a entity with that identifier exists, false otherwise.
   */
  boolean has(Identifier entityId);

  /**
   * Adds entity to the database.
   *
   * @param e given entity to be added.
   * @return true if entity did not exist on the database, false if entity is already in
   * database.
   */
  boolean add(Entity e) throws CodecException;

  /**
   * Removes entity with given identifier.
   *
   * @param e identifier of the entity.
   * @return true if entity exists on database and removed successfully, false if entity does not exist on
   * database.
   */
  boolean remove(Entity e) throws CodecException;

  /**
   * Returns the entity with given identifier.
   *
   * @param e identifier of the entity.
   * @return the entity itself if exists and null otherwise.
   */
  Entity get(Identifier e) throws CodecException;

  /**
   * Returns all entities stored in database.
   *
   * @return all stored entities in database.
   */
  ArrayList<Entity> all() throws CodecException;
}
