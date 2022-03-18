package storage;

import java.util.ArrayList;

import model.lightchain.Identifier;

/**
 * Represents a persistent key-value storage for identifiers on disk.
 */
public interface Identifiers {
  /**
   * Adds an identifier to the storage, returns true if it is new, false if it already exists.
   *
   * @param identifier identifier to be added to storage.
   * @return true if it is new, false if it already exists.
   */
  boolean add(Identifier identifier);

  /**
   * Checks existence of an identifier on the storage.
   *
   * @param identifier identifier to be checked.
   * @return true if identifier exists on the storage, false otherwise.
   */
  boolean has(Identifier identifier);

  /**
   * Removes an identifier from the storage.
   *
   * @param identifier identifier to be removed.
   * @return true if identifier exists and removed, false otherwise.
   */
  boolean remove(Identifier identifier);

  /**
   * Returns all stored identifiers on storage.
   *
   * @return all stored identifiers on storage.
   */
  ArrayList<Identifier> all();
}
