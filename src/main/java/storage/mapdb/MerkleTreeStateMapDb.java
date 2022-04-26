package storage.mapdb;

import model.lightchain.Identifier;
import modules.ads.merkletree.MerkleTreeState;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import storage.MerkleTreeStates;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MerkleTreeStateMapDb implements MerkleTreeStates {
  private final DB db;
  private final ReentrantReadWriteLock lock;
  private static final String MAP_NAME = "merkle_tree_state_map";
  private final HTreeMap<byte[], byte[]> merkleTreeStateMap;

  /**
   * Creates MapDb.
   *
   * @param filePath the path of the file.
   */
  public MerkleTreeStateMapDb(String filePath) {
    this.db = DBMaker.fileDB(filePath).make();
    this.lock = new ReentrantReadWriteLock();
    merkleTreeStateMap = this.db.hashMap(MAP_NAME)
        .keySerializer(Serializer.BYTE_ARRAY)
        .valueSerializer(Serializer.BYTE_ARRAY)
        .createOrOpen();
  }

  /**
   * Adds a merkle tree state to the storage, returns true if it is new, false if it already exists.
   *
   * @param merkleTreeState merkle tree state to be added to storage.
   * @return true if it is new, false if it already exists.
   */
  @Override
  public boolean add(MerkleTreeState merkleTreeState) {
    boolean addBoolean;
    try {
      lock.writeLock().lock();
      addBoolean = merkleTreeStateMap.putIfAbsentBoolean(merkleTreeState.getBytes(), merkleTreeState.getBytes());
    } finally {
      lock.writeLock().unlock();
    }
    return addBoolean;
  }

  /**
   * Checks existence of a merkle tree state on the storage.
   *
   * @param merkleTreeState merkle tree state to be checked.
   * @return true if it exists on the storage, false otherwise.
   */
  @Override
  public boolean has(MerkleTreeState merkleTreeState) {
    boolean hasBoolean;
    try {
      lock.readLock().lock();
      hasBoolean = merkleTreeStateMap.containsKey(merkleTreeState.getBytes());
    } finally {
      lock.readLock().unlock();
    }
    return hasBoolean;
  }

  /**
   * Removes a merkle tree state from the storage.
   *
   * @param merkleTreeState merkle tree state to be removed.
   * @return true if it exists and removed, false otherwise.
   */
  @Override
  public boolean remove(MerkleTreeState merkleTreeState) {
    return merkleTreeStateMap.remove(merkleTreeState.getBytes(), merkleTreeState.getBytes());
  }

  /**
   * Returns all stored merkle tree state on storage.
   *
   * @return all stored merkle tree state on storage.
   */
  @Override
  public ArrayList<MerkleTreeState> all() {
    ArrayList<MerkleTreeState> states;
      states = new ArrayList<>();
      for (byte[] element : merkleTreeStateMap.keySet()) {
        try {
          ByteArrayInputStream bis = new ByteArrayInputStream(element);
          ObjectInputStream inp = null;
          inp = new ObjectInputStream(bis);
          MerkleTreeState state = (MerkleTreeState) inp.readObject();
          states.add(state);
        } catch (IOException e) {
          e.printStackTrace();
        } catch (ClassNotFoundException e) {
          throw new RuntimeException(e);
        }
      }
    return states;
  }

  /**
   * Closes the database.
   */
  public void closeDb() {
    db.close();
  }
}
