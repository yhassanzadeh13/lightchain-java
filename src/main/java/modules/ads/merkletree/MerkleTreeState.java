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
import storage.MerkleTreeStates;

/**
 * This class represents the state of the Merkle Tree.
 */
public class MerkleTreeState implements Serializable {
  private ArrayList<MerkleNode> leafNodes;
  private Map<Sha3256Hash, Integer> leafNodesHashTable;
  private Map<Identifier, Entity> entityHashTable;

  /**
   * Constructor.
   */
  public MerkleTreeState() {
    this.leafNodes = new ArrayList<>();
    this.leafNodesHashTable = new HashMap<>();
    this.entityHashTable = new HashMap<>();
  }

  /**
   * Constructor.
   */
  public MerkleTreeState(MerkleTreeState state) {
    this.leafNodes = state.getLeafNodes();
    this.leafNodesHashTable = state.getLeafNodesHashTable();
    this.entityHashTable = state.getEntityHashTable();
  }

  /**
   * Returns the leaf nodes of the Merkle Tree.
   *
   * @return the leaf nodes of the Merkle Tree
   */
  public ArrayList<MerkleNode> getLeafNodes() {
    return new ArrayList<>(leafNodes);
  }

  /**
   * Returns the hash table of the leaf nodes.
   *
   * @return the hash table of the leaf nodes
   */
  public Map<Sha3256Hash, Integer> getLeafNodesHashTable() {
    return new HashMap<>(leafNodesHashTable);
  }

  /**
   * Returns the hash table of the entities.
   *
   * @return the hash table of the entities
   */
  public Map<Identifier, Entity> getEntityHashTable() {
    return new HashMap<>(entityHashTable);
  }

  /**
   * Adds a leaf node to the Merkle Tree.
   *
   * @param node the leaf node to be added
   */
  public void addLeafNode(MerkleNode node) {
    this.leafNodes.add(node);
  }

  /**
   * Adds a leaf node to the hash table of the leaf nodes.
   *
   * @param hash the hash of the leaf node
   * @param idx  the index of the leaf node
   */
  public void putLeafNodeHash(Sha3256Hash hash, Integer idx) {
    this.leafNodesHashTable.put(hash, idx);
  }

  /**
   * Adds an entity to the hash table of the entities.
   *
   * @param id the identifier of the entity
   * @param e  the entity
   */
  public void putEntityHashTable(Identifier id, Entity e) {
    this.entityHashTable.put(id, e);
  }

  /**
   * Returns the index of the node.
   *
   * @return the index of the node
   */
  public int getNodeIndex(Sha3256Hash hash) {
    if (this.leafNodesHashTable.get(hash) != null) {
      return this.leafNodesHashTable.get(hash);
    }
    return -1;
  }

  /**
   * Returns the entity from hash table with a given id.
   *
   * @return the entity
   */
  public Entity getEntity(Identifier id) {
    if (this.entityHashTable.get(id) != null) {
      return this.entityHashTable.get(id);
    }
    return null;
  }

  /**
   * Returns the node from its index.
   *
   * @param idx the index of the node
   *
   * @return the node
   */
  public MerkleNode getNode(int idx) {
    return this.leafNodes.get(idx);
  }

  /**
   * Returns if o is equal to this.
   *
   * @param o the object to be compared
   *
   * @return true if o is equal to this, false otherwise
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MerkleTreeState that = (MerkleTreeState) o;
    return leafNodes.equals(that.leafNodes);
  }

  /**
   * Returns the hash code of this.
   *
   * @return the hash code of this
   */
  @Override
  public int hashCode() {
    return Objects.hash(leafNodes);
  }

  /**
   * Returns a byte array representation of this.
   *
   * @return a byte array representation of this
   */
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
