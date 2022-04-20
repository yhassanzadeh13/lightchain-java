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
import modules.ads.AuthenticatedEntity;

/**
 * Implementation of an in-memory Authenticated Skip List
 * that is capable of storing and retrieval of LightChain entities.
 */
public class MerkleTree implements AuthenticatedDataStructure {
  private static final Sha3256Hasher hasher = new Sha3256Hasher();
  private final ReentrantReadWriteLock lock;
  private final ArrayList<MerkleNode> leafNodes;
  private final Map<Sha3256Hash, Integer> leafNodesHashTable;
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
  }

  @Override
  public AuthenticatedEntity put(Entity e) throws IllegalArgumentException {
    lock.writeLock().lock();
    if (e == null) {
      lock.writeLock().unlock();
      throw new IllegalArgumentException("Entity cannot be null");
    }
    Sha3256Hash hash = hasher.computeHash(e.id());
    Integer idx = leafNodesHashTable.get(hash);
    if (idx == null) {
      leafNodes.add(new MerkleNode(e, false));
      leafNodesHashTable.put(hash, size);
      size++;
      buildMerkleTree();
      Proof proof = getProof(e.id());
      lock.writeLock().unlock();
      return new AuthenticatedLightChainEntity(proof, e.type(), e);
    } else {
      Proof proof = getProof(e.id());
      lock.writeLock().unlock();
      return new AuthenticatedLightChainEntity(proof, e.type(), e);
    }
  }

  @Override
  public AuthenticatedEntity get(Entity e) throws IllegalArgumentException {
    Proof proof;
    if (e == null) {
      throw new IllegalArgumentException("Entity cannot be null");
    }
    lock.readLock().lock();
    try {
      proof = getProof(e.id());
    } finally {
      lock.readLock().unlock();
    }
    if (proof == null) {
      return null;
    }
    return new AuthenticatedLightChainEntity(proof, e.type(), e);
  }

  private Proof getProof(Identifier id) {
    Integer idx = leafNodesHashTable.get(hasher.computeHash(id));
    if (idx == null) {
      return null;
    }
    ArrayList<Sha3256Hash> path = new ArrayList<>();
    MerkleNode currNode = leafNodes.get(idx);
    while (currNode != root) {
      path.add(currNode.getSibling().getHash());
      currNode = currNode.getParent();
    }
    return new Proof(path, root.getHash());
  }

  private void buildMerkleTree() {
    ArrayList<MerkleNode> parentNodes = new ArrayList<>();
    ArrayList<MerkleNode> childNodes = new ArrayList<>(leafNodes);
    while (childNodes.size() > 1) {
      int idx = 0;
      int len = childNodes.size();
      while (idx < len) {
        MerkleNode left = childNodes.get(idx);
        left.setLeft(true);
        MerkleNode right = null;
        if (idx + 1 < len) {
          right = childNodes.get(idx + 1);
        } else {
          right = new MerkleNode(left.getHash());
        }
        Sha3256Hash hash = hasher.computeHash(left.getHash().getBytes(), right.getHash().getBytes());
        MerkleNode parent = new MerkleNode(hash, left, right);
        left.setParent(parent);
        right.setParent(parent);
        parentNodes.add(parent);
        idx += 2;
      }
      childNodes = parentNodes;
      parentNodes = new ArrayList<>();
    }
    root = childNodes.get(0);
  }
}
