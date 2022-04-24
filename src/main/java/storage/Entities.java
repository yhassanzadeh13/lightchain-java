package storage;

import java.util.ArrayList;

import model.Entity;

public interface Entities {
  /**
   * Adds an entity to the storage, returns true if it is new, false if it already exists.
   *
   * @param entity entity to be added to storage.
   * @return true if it is new, false if it already exists.
   */
  boolean add(Entity entity);

  /**
   * Checks existence of an entity on the storage.
   *
   * @param entity entity to be checked.
   * @return true if entity exists on the storage, false otherwise.
   */
  boolean has(Entity entity);

  /**
   * Removes an entity from the storage.
   *
   * @param entity entity to be removed.
   * @return true if entity exists and removed, false otherwise.
   */
  boolean remove(Entity entity);

  /**
   * Returns all stored entity on storage.
   *
   * @return all stored entity on storage.
   */
  ArrayList<Entity> all();
}
