package modules.ads.mtrie;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import crypto.Sha3256Hasher;
import model.Entity;
import model.lightchain.Identifier;
import modules.ads.AuthenticatedEntity;
import modules.ads.merkletree.MerkleTree;

public class MerklePatriciaTrie extends MerkleTree {
  private static final Sha3256Hasher hasher = new Sha3256Hasher();
  private final ReentrantReadWriteLock lock;
  private final MerklePatriciaBranchNode root;

  public MerklePatriciaTrie(ReentrantReadWriteLock lock, MerklePatriciaBranchNode root) {
    this.lock = lock;
    this.root = root;
  }

  /**
   * Adds an entity to the ADS.
   *
   * @param e the entity to add
   * @return AuthenticatedEntity containing the entity and its membership proof
   */
  @Override
  public AuthenticatedEntity put(Entity e) {
    int idx;
    MerklePatriciaBranchNode currBranchNode = root;
    byte[] bytes = e.id().getBytes();
    for (int i = 0; i < bytes.length - 1; i++) {
      idx = bytes[i] + 128;
      currBranchNode = currBranchNode.getChild(idx);
    }
    idx = bytes[bytes.length - 1] + 128;
    currBranchNode.createLeaf(idx, e.id());
    return null;
  }

  /**
   * Returns the AuthenticatedEntity corresponding to the given identifier.
   *
   * @param id the identifier of the entity to retrieve
   * @return the AuthenticatedEntity corresponding to the given identifier
   */
  @Override
  public AuthenticatedEntity get(Identifier id) {
    return null;
  }

  /**
   * Returns the size of the ADS.
   *
   * @return the size of the ADS
   */
  @Override
  public int size() {
    return 0;
  }
}
