package modules.ads.merkletree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import crypto.Sha3256Hasher;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import model.Entity;
import model.crypto.Sha3256Hash;
import model.lightchain.Identifier;
import modules.ads.AuthenticatedDataStructure;
import storage.mapdb.MerkleTreeStateMapDb;

/**
 * On disk MerkleTree class.
 */
public class MerkleTreeOnDiskState implements AuthenticatedDataStructure, MerkleTree, Serializable {
  private static final Sha3256Hasher hasher = new Sha3256Hasher();
  private final ReentrantReadWriteLock lock;
  private MerkleTreeStateMapDb stateMapDb;
  private int size;
  private MerkleNode root;
  private MerkleTreeState state;

  /**
   * Default constructor for an on disk Merkle Tree.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "state is intentionally mutable externally")
  public MerkleTreeOnDiskState(MerkleTreeStateMapDb stateMapDb) {
    this.size = 0;
    this.root = new MerkleNode();
    this.lock = new ReentrantReadWriteLock();
    this.stateMapDb = stateMapDb;
    if (stateMapDb.isEmpty()) {
      this.state = new MerkleTreeState();
      this.stateMapDb.add(this.state);
    } else {
      this.state = stateMapDb.getLatest();
    }
  }

  /**
   * Puts the given entity into the merkle tree and return AuthenticationEntity.
   *
   * @param e the entity to be put into the merkle tree
   *
   * @return AuthenticationEntity of the given entity
   * @throws IllegalArgumentException if the entity is null
   */
  public modules.ads.AuthenticatedEntity put(Entity e) throws IllegalArgumentException {
    try {
      lock.writeLock().lock();
      if (e == null) {
        throw new IllegalArgumentException("entity cannot be null");
      }
      int idx = state.getNodeIndex(new Sha3256Hash(e.id().getBytes()));
      if (idx == -1) {
        MerkleTreeState newState = put(e, state, size);
        stateMapDb.changeTo(state, newState);
        this.state = newState;
        size++;
        buildMerkleTree();
      }
      MerkleProof proof = getProof(e.id(), state, root);
      return new MerkleTreeAuthenticatedEntity(proof, e.type(), e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Return the AuthenticationEntity of the given identifier.
   *
   * @param id the identifier whose AuthenticationEntity is to be returned
   *
   * @return the AuthenticationEntity of the given identifier
   * @throws IllegalArgumentException if the identifier is not found
   */
  public modules.ads.AuthenticatedEntity get(Identifier id) throws IllegalArgumentException {
    MerkleProof proof;
    if (id == null) {
      throw new IllegalArgumentException("identifier cannot be null");
    }
    try {
      lock.readLock().lock();
      proof = getProof(id, state, root);
      Entity e = state.getEntity(id);
      return new MerkleTreeAuthenticatedEntity(proof, e.type(), e);
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Updates the merkle tree after a new entity is added.
   */
  private void buildMerkleTree() {
    // keeps nodes of the current level of the merkle tree
    // will be updated bottom up
    // initialized with leaves
    ArrayList<MerkleNode> currentLevelNodes = new ArrayList<>(state.getLeafNodes());

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

  /**
   * Returns the size of the ADS.
   *
   * @return the size of the ADS
   */
  public int size() {
    return this.size;
  }
}
