package modules.ads.mtrie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import crypto.Sha3256Hasher;
import model.Entity;
import model.crypto.Sha3256Hash;
import model.lightchain.Identifier;
import modules.ads.AuthenticatedEntity;
import modules.ads.merkletree.*;

/**
 * Implementation of an in-memory Merkle Patricia Tree
 * that is capable of storing and retrieval of LightChain entities.
 */
public class MerklePatriciaTrie extends MerkleTree {
  private static final Sha3256Hasher hasher = new Sha3256Hasher();
  private final Map<Identifier, Entity> entityHashTable;
  private final ReentrantReadWriteLock lock;
  private final MerkleNode root;
  private int size;

  /**
   * Default constructor for a Merkle Patricia Trie.
   */
  public MerklePatriciaTrie() {
    this.size = 0;
    this.root = new MerkleNode();
    this.entityHashTable = new HashMap<>();
    this.lock = new ReentrantReadWriteLock();
  }

  /**
   * Adds an entity to the merkle patricia tree.
   *
   * @param e the entity to add
   * @return AuthenticatedEntity containing the entity and its membership proof
   */
  @Override
  public AuthenticatedEntity put(Entity e) throws IllegalArgumentException {
    try {
      lock.writeLock().lock();
      if (e == null) {
        throw new IllegalArgumentException("entity cannot be null");
      }
      MerkleNode currNode = root;
      Identifier id = e.id();
      byte[] bytes = id.getBytes();
      String path = "";
      for (byte b : bytes) {
        path += String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
      }
      for (int i = 0; i < path.length() - 1; i++) {
        if (path.charAt(i) == '0') {
          if (currNode.getLeft() == null) {
            currNode.setLeftNode(new MerkleNode(currNode, true));
          }
          currNode = currNode.getLeft();
        } else {
          if (currNode.getRight() == null) {
            currNode.setRightNode(new MerkleNode(currNode, false));
          }
          currNode = currNode.getRight();
        }
      }
      MerkleNode newNode = null;
      if (path.charAt(path.length() - 1) == '0') {
        if (currNode.getLeft() == null) {
          newNode = new MerkleNode(currNode, true, hasher.computeHash(id));
          currNode.setLeftNode(newNode);
          entityHashTable.put(id, e);
          size++;
        }
      } else {
        if (currNode.getRight() == null) {
          newNode = new MerkleNode(currNode, false, hasher.computeHash(id));
          currNode.setRightNode(newNode);
          entityHashTable.put(id, e);
          size++;
        }
      }
      for (int i = 0; i < path.length() - 1; i++) {
        currNode.updateHash();
        currNode = currNode.getParent();
      }
      currNode.updateHash();
      return get(id);
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Returns the AuthenticatedEntity corresponding to the given identifier.
   *
   * @param id the identifier of the entity to retrieve
   * @return the AuthenticatedEntity corresponding to the given identifier
   */
  @Override
  public AuthenticatedEntity get(Identifier id) throws IllegalArgumentException {
    try {
      lock.readLock().lock();
      if (id == null) {
        throw new IllegalArgumentException("identifier cannot be null");
      }

      ArrayList<Sha3256Hash> pathList = new ArrayList<>();
      ArrayList<Boolean> isLeftNode = new ArrayList<>();
      MerkleNode currNode = root;
      byte[] bytes = id.getBytes();
      String path = "";
      for (byte b : bytes) {
        path += String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
      }
      for (int i = 0; i < path.length(); i++) {
        if (path.charAt(i) == '0') {
          currNode = currNode.getLeft();
          isLeftNode.add(true);
        } else {
          currNode = currNode.getRight();
          isLeftNode.add(false);
        }
        if (currNode == null) {
          throw new IllegalArgumentException("identifier not found");
        }
        pathList.add(currNode.getSibling() == null ? new Sha3256Hash(new byte[32]) : currNode.getSibling().getHash());
      }
      Collections.reverse(pathList);
      Collections.reverse(isLeftNode);
      Entity e = entityHashTable.get(id);
      MerklePath merklePath = new MerklePath(pathList, isLeftNode);
      MerkleProof proof = new MerkleProof(root.getHash(), merklePath);
      return new MerkleTreeAuthenticatedEntity(proof, e.type(), e);
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Returns the size of the merkle patricia trie.
   *
   * @return the size of the merkle patricia trie
   */
  @Override
  public int size() {
    return size;
  }
}
