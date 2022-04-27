package unittest.fixtures;

import java.util.ArrayList;

import modules.ads.merkletree.MerkleTreeInMemoryState;

/**
 * Creates a new randomly looking MerkleTree.
 */
public class MerkleTreeFixture {

  /**
   * Creates a new merkle tree with n random elements.
   *
   * @param n number of elements to create
   *
   * @return a new merkle tree with n random elements.
   */
  public static MerkleTreeInMemoryState createInMemoryStateMerkleTree(int n) {
    MerkleTreeInMemoryState merkleTree = new MerkleTreeInMemoryState();
    for (int i = 0; i < n; i++) {
      merkleTree.put(new EntityFixture());
    }
    return merkleTree;
  }

  /**
   * Creates n new merkle trees with m random elements.
   *
   * @param n number of trees to create
   * @param m number of elements in each tree
   *
   * @return an array list of n merkle trees with m random elements.
   */
  public static ArrayList<MerkleTreeInMemoryState> newMerkleTrees(int n, int m) {
    ArrayList<MerkleTreeInMemoryState> trees = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      trees.add(createInMemoryStateMerkleTree(m));
    }
    return trees;
  }
}
