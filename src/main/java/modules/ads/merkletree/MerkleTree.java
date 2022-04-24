package modules.ads.merkletree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import crypto.Sha3256Hasher;
import model.Entity;
import model.crypto.Sha3256Hash;
import model.lightchain.Identifier;
import modules.ads.AuthenticatedDataStructure;

/**
 * Implementation of an in-memory Authenticated Skip List
 * that is capable of storing and retrieval of LightChain entities.
 */
public class MerkleTree implements AuthenticatedDataStructure {
  private static final Sha3256Hasher hasher = new Sha3256Hasher();
  private final ReentrantReadWriteLock lock;
  private final ArrayList<MerkleNode> leafNodes;
  private final Map<Sha3256Hash, Integer> leafNodesHashTable;
  private final Map<Identifier, Entity> entityHashTable;
  private int size;
  private MerkleNode root;

  /**
   * Default constructor for a Merkle Tree.
   */
  public MerkleTree() {
    this.size = 0;
    this.root = new MerkleNode();
    this.leafNodes = new ArrayList<>();
    this.lock = new ReentrantReadWriteLock();
    this.leafNodesHashTable = new HashMap<>();
    this.entityHashTable = new HashMap<>();
  }

  @Override
  public modules.ads.AuthenticatedEntity put(Entity e) throws IllegalArgumentException {
    try {
      lock.writeLock().lock();
      if (e == null) {
        throw new IllegalArgumentException("entity cannot be null");
      }
      Sha3256Hash hash = new Sha3256Hash(e.id().getBytes());
      Integer idx = leafNodesHashTable.get(hash);
      if (idx == null) {
        leafNodes.add(new MerkleNode(e, false));
        leafNodesHashTable.put(hash, size);
        entityHashTable.put(e.id(), e);
        size++;
        buildMerkleTree();
        MerkleProof proof = getProof(e.id());
        return new MerkleTreeAuthenticatedEntity(proof, e.type(), e);
      } else {
        MerkleProof proof = getProof(e.id());
        return new MerkleTreeAuthenticatedEntity(proof, e.type(), e);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public modules.ads.AuthenticatedEntity get(Identifier id) throws IllegalArgumentException {
    MerkleProof proof;
    if (id == null) {
      throw new IllegalArgumentException("identifier cannot be null");
    }
    try {
      lock.readLock().lock();
      proof = getProof(id);
      Entity e = entityHashTable.get(id);
      return new MerkleTreeAuthenticatedEntity(proof, e.type(), e);
    } finally {
      lock.readLock().unlock();
    }
  }

  private MerkleProof getProof(Identifier id) throws IllegalArgumentException {
    ArrayList<Boolean> isLeftNode = new ArrayList<>();
    Sha3256Hash hash = new Sha3256Hash(id.getBytes());
    Integer idx = leafNodesHashTable.get(hash);
    if (idx == null) {
      throw new IllegalArgumentException("identifier not found");
    }
    ArrayList<Sha3256Hash> path = new ArrayList<>();
    MerkleNode currentNode = leafNodes.get(idx);
    while (currentNode != root) {
      path.add(currentNode.getSibling().getHash());
      isLeftNode.add(currentNode.isLeft());
      currentNode = currentNode.getParent();
    }
    return new MerkleProof(path, root.getHash(), isLeftNode);
  }

  private void buildMerkleTree() {
    // keeps nodes of the current level of the merkle tree
    // will be updated bottom up
    // initialized with leaves
    ArrayList<MerkleNode> currentLevelNodes = new ArrayList<>(leafNodes);

    // keeps nodes of the next level of merkle tree
    // used as an intermediary data structure.
    ArrayList<MerkleNode> nextLevelNodes = new ArrayList<>();

    while (currentLevelNodes.size() > 1) { // more than one current node, means we have not yet reached root.
      for (int i = 0; i < currentLevelNodes.size(); i += 2) {
        // pairs up current level nodes as siblings for next level.
        MerkleNode left = currentLevelNodes.get(i);
        left.setLeft(true);

        MerkleNode right;
        if (i + 1 < currentLevelNodes.size()) {
          right = currentLevelNodes.get(i + 1); // we have a right node
        } else {
          // TODO: edge case need to get fixed.
          right = new MerkleNode(left.getHash());
        }
        Sha3256Hash hash = hasher.computeHash(left.getHash().getBytes(), right.getHash().getBytes());
        MerkleNode parent = new MerkleNode(hash, left, right);
        left.setParent(parent);
        right.setParent(parent);
        nextLevelNodes.add(parent);
      }
      currentLevelNodes = nextLevelNodes;
      nextLevelNodes = new ArrayList<>();
    }
    root = currentLevelNodes.get(0);
  }

  public int size() {
    return this.size;
  }

  /**
   * returns all entities in the merkle tree.
   *
   * @return all entities in the merkle tree.
   */
  public ArrayList<Entity> all() {
    ArrayList<Entity> entities = new ArrayList<>();
    for (Entity e : entityHashTable.values()) {
      entities.add(e);
    }
    return entities;
  }
}
