package storage;

import java.util.ArrayList;

import modules.ads.merkletree.MerkleTreeState;

/**
 * Interface for the Merkle Tree States.
 */
public interface MerkleTreeStates {

  /**
   * Adds a merkle tree state to the storage, returns true if it is new, false if it already exists.
   *
   * @param merkleTreeState merkle tree state to be added to storage.
   *
   * @return true if it is new, false if it already exists.
   */
  boolean add(MerkleTreeState merkleTreeState);

  /**
   * Checks existence of a merkle tree state on the storage.
   *
   * @param merkleTreeState merkle tree state to be checked.
   *
   * @return true if it exists on the storage, false otherwise.
   */
  boolean has(MerkleTreeState merkleTreeState);

  /**
   * Removes a merkle tree state from the storage.
   *
   * @param merkleTreeState merkle tree state to be removed.
   *
   * @return true if it exists and removed, false otherwise.
   */
  boolean remove(MerkleTreeState merkleTreeState);

  /**
   * Returns all stored merkle tree state on storage.
   *
   * @return all stored merkle tree state on storage.
   */
  ArrayList<MerkleTreeState> all();
}
