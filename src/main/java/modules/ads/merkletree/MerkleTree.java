package modules.ads.merkletree;

import model.Entity;
import model.crypto.Sha3256Hash;
import model.lightchain.Identifier;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public interface MerkleTree {

  /**
   * Returns the membership proof of the given identifier.
   *
   * @param id the identifier whose membership proof is to be returned
   * @param state the state of the merkle tree
   * @param root the root of the merkle tree
   *
   * @return the membership proof of the given identifier
   * @throws IllegalArgumentException if the given identifier is not in the merkle tree
   */
  default MerkleProof getProof(Identifier id, MerkleTreeState state, MerkleNode root) throws IllegalArgumentException {
    ArrayList<Boolean> isLeftNode = new ArrayList<>();
    Sha3256Hash hash = new Sha3256Hash(id.getBytes());
    int idx = state.getNodeIndex(hash);
    if (idx == -1) {
      throw new IllegalArgumentException("identifier not found");
    }
    ArrayList<Sha3256Hash> path = new ArrayList<>();
    MerkleNode currentNode = state.getNode(idx);
    while (currentNode != root) {
      path.add(currentNode.getSibling().getHash());
      isLeftNode.add(currentNode.isLeft());
      currentNode = currentNode.getParent();
    }
    return new MerkleProof(path, root.getHash(), isLeftNode);
  }

  /**
   * Puts the given entity into the state and returns the state.
   *
   * @param e the entity to be put into the state
   * @param state the state to be updated
   * @param size the size of the state
   *
   * @return the updated state
   */
  default MerkleTreeState put(Entity e, MerkleTreeState state, int size) {
    Sha3256Hash hash = new Sha3256Hash(e.id().getBytes());
    int idx = state.getNodeIndex(hash);
    if (idx == -1) {
      state.addLeafNode(new MerkleNode(e, false));
      state.putLeafNodeHash(hash, size);
      state.putEntityHashTable(e.id(), e);
    }
    return state;
  }

  /**
   * Puts the given entity into the merkle tree and return AuthenticationEntity.
   *
   * @param e the entity to be put into the merkle tree
   *
   * @return AuthenticationEntity of the given entity
   * @throws IllegalArgumentException if the entity is null
   */
  modules.ads.AuthenticatedEntity put(Entity e) throws IllegalArgumentException;

  /**
   * Return the AuthenticationEntity of the given identifier.
   *
   * @param id the identifier whose AuthenticationEntity is to be returned
   *
   * @return the AuthenticationEntity of the given identifier
   * @throws IllegalArgumentException if the identifier is not found
   */
  modules.ads.AuthenticatedEntity get(Identifier id) throws IllegalArgumentException;

  /**
   * Returns the byte array of the merkle tree.
   *
   * @return the byte array of the merkle tree
   */
  default byte[] getBytes() {
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

  /**
   * Returns the size of the ADS.
   *
   * @return the size of the ADS
   */
  int size();

}
