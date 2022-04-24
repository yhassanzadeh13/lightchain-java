package storage.mapdb;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.gson.Gson;
import modules.ads.merkletree.MerkleTree;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import storage.MerkleTrees;

public class MerkleTreeMapDb implements MerkleTrees {
  private final DB db;
  private final ReentrantReadWriteLock lock;
  private static final String MAP_NAME = "merkle_tree_map";
  private final HTreeMap<byte[], byte[]> merkleTreeMap;

  public MerkleTreeMapDb(String filePath) {
    this.db = DBMaker.fileDB(filePath).make();
    this.lock = new ReentrantReadWriteLock();
    this.merkleTreeMap = db.hashMap(MAP_NAME).
            keySerializer(Serializer.BYTE_ARRAY)
            .valueSerializer(Serializer.BYTE_ARRAY)
            .createOrOpen();
  }

  @Override
  public boolean add(MerkleTree tree) {
    boolean addBoolean;
    try {
      lock.writeLock().lock();
      addBoolean = merkleTreeMap.putIfAbsentBoolean(tree.getBytes(), tree.getBytes());
    } finally {
      lock.writeLock().unlock();
    }
    return addBoolean;
  }

  @Override
  public boolean has(MerkleTree tree) {
    boolean hasBoolean;
    try {
      lock.readLock().lock();
      hasBoolean = merkleTreeMap.containsKey(tree.getBytes());
    } finally {
      lock.readLock().unlock();
    }
    return hasBoolean;
  }

  @Override
  public boolean remove(MerkleTree tree) {
    return merkleTreeMap.remove(tree.getBytes(), tree.getBytes());
  }

  @Override
  public ArrayList<MerkleTree> all() {
    ArrayList<MerkleTree> trees = new ArrayList<>();
    for (byte[] element : merkleTreeMap.keySet()) {
      try {
      ByteArrayInputStream bis = new ByteArrayInputStream(element);
      ObjectInputStream inp = null;
      inp = new ObjectInputStream(bis);
      MerkleTree tree = (MerkleTree) inp.readObject();
      /*
      Gson gson = new Gson();
      String json = new String(element.clone(), StandardCharsets.UTF_8);
      MerkleTree tree = gson.fromJson(json, MerkleTree.class);
       */
      trees.add(tree);
      } catch (IOException e) {
        e.printStackTrace();
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
    return trees;
  }

  public void closeDb() {
    db.close();
  }
}
