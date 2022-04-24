package storage.mapdb;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.gson.Gson;
import modules.ads.merkletree.MerkleNode;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import storage.MerkleNodes;

public class MerkleNodeMapDb implements MerkleNodes {
  private final DB db;
  private final ReentrantReadWriteLock lock;
  private static final String MAP_NAME = "merkle_node_map";
  private final HTreeMap<byte[], byte[]> merkleNodeMap;

  public MerkleNodeMapDb(String filePath) {
    this.db = DBMaker.fileDB(filePath).make();
    this.lock = new ReentrantReadWriteLock();
    this.merkleNodeMap = db.hashMap(MAP_NAME).
            keySerializer(Serializer.BYTE_ARRAY)
            .valueSerializer(Serializer.BYTE_ARRAY)
            .createOrOpen();
  }

  @Override
  public boolean add(MerkleNode node) {
    boolean addBoolean;
    try {
      lock.writeLock().lock();
      addBoolean = merkleNodeMap.putIfAbsentBoolean(node.getBytes(), node.getBytes());
    } finally {
      lock.writeLock().unlock();
    }
    return addBoolean;
  }

  @Override
  public boolean has(MerkleNode node) {
    boolean hasBoolean;
    try {
      lock.readLock().lock();
      hasBoolean = merkleNodeMap.containsKey(node.getBytes());
    } finally {
      lock.readLock().unlock();
    }
    return hasBoolean;
  }

  @Override
  public boolean remove(MerkleNode node) {
    return merkleNodeMap.remove(node.getBytes(), node.getBytes());
  }

  @Override
  public ArrayList<MerkleNode> all() {
    ArrayList<MerkleNode> nodes = new ArrayList<>();
    for (byte[] element : merkleNodeMap.keySet()) {
      Gson gson = new Gson();
      String json = new String(element.clone(), StandardCharsets.UTF_8);
      MerkleNode node = gson.fromJson(json, MerkleNode.class);
      nodes.add(node);
    }
    return nodes;
  }
}
