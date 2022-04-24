package unittest.fixtures;

import java.util.ArrayList;

import model.lightchain.Identifier;
import modules.ads.merkletree.MerkleTree;

/**
 * Creates a new randomly looking MerkleTree.
 */
public class MerkleTreeFixture {
  /**
   * Creates a new merkle tree with n random elements.
   *
   * @param n number of elements to create
   * @return a new merkle tree with n random elements.
   */
  public static MerkleTree createMerkleTree(int n) {
    MerkleTree merkleTree = new MerkleTree();
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
   * @return an array list of n merkle trees with m random elements.
   */
  public static ArrayList<MerkleTree> newMerkleTrees(int n, int m) {
    ArrayList<MerkleTree> trees = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      trees.add(createMerkleTree(m));
    }
    return trees;
  }
}
