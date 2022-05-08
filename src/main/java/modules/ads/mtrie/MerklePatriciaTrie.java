package modules.ads.mtrie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import crypto.Sha3256Hasher;
import model.Entity;
import model.crypto.Sha3256Hash;
import model.lightchain.Identifier;
import modules.ads.AuthenticatedEntity;
import modules.ads.merkletree.*;

public class MerklePatriciaTrie extends MerkleTree {
  private static final Sha3256Hasher hasher = new Sha3256Hasher();
  private int size;
  private MerkleNode root;
  private final Map<Identifier, Entity> entityHashTable;

  /**
   * Default constructor for a Merkle Tree.
   */
  public MerklePatriciaTrie() {
    this.size = 0;
    this.root = new MerkleNode();
    this.entityHashTable = new HashMap<>();
  }

  /**
   * Adds an entity to the merkle tree.
   *
   * @param e the entity to add
   *
   * @return AuthenticatedEntity containing the entity and its membership proof
   */
  @Override
  public AuthenticatedEntity put(Entity e) throws IllegalArgumentException {
    MerkleNode currNode = root;
    Identifier id = e.id();
    byte[] bytes = id.getBytes();
    String path = "";
    for (byte b : bytes) {
      path += String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
    }
    for (int i = 0; i < path.length() - 1; i++) {
      if (path.charAt(i) == '1') {
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
    if (path.charAt(path.length() - 1) == '1') {
      if (currNode.getLeft() == null) {
        currNode.setLeftNode(new MerkleNode(currNode, true, new Sha3256Hash(id)));
        entityHashTable.put(id, e);
        size++;
      }
    } else {
      if (currNode.getRight() == null) {
        currNode.setRightNode(new MerkleNode(currNode, false, new Sha3256Hash(id)));
        entityHashTable.put(id, e);
        size++;
      }
    }
    for (int i = 0; i < path.length() - 1; i++) {
      currNode.updateHash();
      currNode = currNode.getParent();
    }
    return get(id);
  }

  /**
   * Returns the AuthenticatedEntity corresponding to the given identifier.
   *
   * @param id the identifier of the entity to retrieve
   *
   * @return the AuthenticatedEntity corresponding to the given identifier
   */
  @Override
  public AuthenticatedEntity get(Identifier id) throws IllegalArgumentException {
    ArrayList<Sha3256Hash> pathList = new ArrayList<>();
    ArrayList<Boolean> isLeftNode = new ArrayList<>();
    MerkleNode currNode = root;
    byte[] bytes = id.getBytes();
    String path = "";
    for (byte b : bytes) {
      path += String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
    }
    for (int i = 0; i < path.length(); i++) {
      if (path.charAt(i) == '1') {
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
    Entity e = entityHashTable.get(id);
    MerklePath merklePath = new MerklePath(pathList, isLeftNode);
    MerkleProof proof = new MerkleProof(root.getHash(), merklePath);
    return new MerkleTreeAuthenticatedEntity(proof, e.type(), e);
  }

  /**
   * Returns the size of the merkle tree.
   *
   * @return the size of the merkle tree
   */
  @Override
  public int size() {
    return size;
  }
}
