package unittest.fixtures;

import modules.ads.merkletree.MerkleTree;

/**
 * Creates a new randomly looking MerkleTree.
 */
public class MerkleTreeFixture {
  /**
   * Creates a new skip list with n random elements.
   *
   * @param n number of elements to create
   * @return a new skip list with n random elements
   */
  public static MerkleTree createSkipList(int n) {
    MerkleTree merkleTree = new MerkleTree();
    for (int i = 0; i < n; i++) {
      merkleTree.put(new EntityFixture());
    }
    return merkleTree;
  }
}
