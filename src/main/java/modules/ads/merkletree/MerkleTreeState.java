package modules.ads.merkletree;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import model.Entity;
import model.crypto.Sha3256Hash;
import model.lightchain.Identifier;

public class MerkleTreeState implements Serializable {
  private ArrayList<MerkleNode> leafNodes;
  private Map<Sha3256Hash, Integer> leafNodesHashTable;
  private Map<Identifier, Entity> entityHashTable;

  public MerkleTreeState() {
    this.leafNodes = new ArrayList<>();
    this.leafNodesHashTable = new HashMap<>();
    this.entityHashTable = new HashMap<>();
  }

  public MerkleTreeState(ArrayList<MerkleNode> leafNodes, Map<Sha3256Hash, Integer> leafNodesHashTable, Map<Identifier, Entity> entityHashTable) {
    this.leafNodes = leafNodes;
    this.leafNodesHashTable = leafNodesHashTable;
    this.entityHashTable = entityHashTable;
  }

  public ArrayList<MerkleNode> getLeafNodes() {
    return leafNodes;
  }

  public Map<Sha3256Hash, Integer> getLeafNodesHashTable() {
    return leafNodesHashTable;
  }

  public Map<Identifier, Entity> getEntityHashTable() {
    return entityHashTable;
  }

  public void addLeafNode(MerkleNode node) {
    this.leafNodes.add(node);
  }

  public void putLeafNodeHash(Sha3256Hash hash, Integer idx) {
    this.leafNodesHashTable.put(hash, idx);
  }

  public void putEntityHashTable(Identifier id, Entity e) {
    this.entityHashTable.put(id, e);
  }

  public int getNodeIndex(Sha3256Hash hash) {
    if (this.leafNodesHashTable.get(hash) != null) {
      return this.leafNodesHashTable.get(hash);
    }
    return -1;
  }

  public Entity getEntity(Identifier id) {
    if (this.entityHashTable.get(id) != null) {
      return this.entityHashTable.get(id);
    }
    return null;
  }

  public MerkleNode getNode(int idx) {
    return this.leafNodes.get(idx);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MerkleTreeState that = (MerkleTreeState) o;
    return leafNodes.equals(that.leafNodes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(leafNodes);
  }

  public byte[] getBytes() {
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream out = null;
      out = new ObjectOutputStream(bos);
      out.writeObject(this);
      out.flush();
      byte[] bytes = bos.toByteArray();
      return bytes;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }
}
