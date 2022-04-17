package modules.ads.merkletree;

import crypto.Sha3256Hasher;
import model.Entity;
import model.crypto.Sha3256Hash;
import model.lightchain.Identifier;
import modules.ads.AuthenticatedDataStructure;
import modules.ads.AuthenticatedEntity;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementation of an in-memory Authenticated Skip List
 * that is capable of storing and retrieval of LightChain entities.
 */
public class MerkleTree implements AuthenticatedDataStructure {
  private int size;
  private MerkleNode root;
  private final ReentrantReadWriteLock lock;
  private final ArrayList<MerkleNode> leafNodes;
  private final Map<Sha3256Hash, Integer> leafNodesHashTable;
  private static final Sha3256Hasher hasher = new Sha3256Hasher();

  public MerkleTree() {
    this.size = 0;
    this.root = new MerkleNode();
    this.leafNodes = new ArrayList<>();
    this.lock = new ReentrantReadWriteLock();
    this.leafNodesHashTable = new HashMap<>();
  }

  @Override
  public AuthenticatedEntity put(Entity e) {
    lock.writeLock().lock();
    if (e == null) {
      lock.writeLock().unlock();
      return null;
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
      return new modules.ads.merkletree.AuthenticatedEntity(proof, e.type(), e);
    } else {
      Proof proof = getProof(e.id());
      lock.writeLock().unlock();
      return new modules.ads.merkletree.AuthenticatedEntity(proof, e.type(), e);
    }
  }

  @Override
  public AuthenticatedEntity get(Entity e) {
    lock.readLock().lock();
    Proof proof = getProof(e.id());
    if (proof == null) {
      lock.readLock().unlock();
      return null;
    }
    lock.readLock().unlock();
    return new modules.ads.merkletree.AuthenticatedEntity(proof, e.type(), e);
  }

  private Proof getProof(Identifier id) {
    if (id == null) {
      return null;
    }
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
