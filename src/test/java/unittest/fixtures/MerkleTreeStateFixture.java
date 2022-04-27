package unittest.fixtures;

import java.util.ArrayList;

import modules.ads.merkletree.MerkleTreeInMemoryState;
import modules.ads.merkletree.MerkleTreeState;

/**
 * Encapsulates utilities for a merkle tree state.
 */
public class MerkleTreeStateFixture {
  /**
   * Creates a new MerkleTreeInMemoryState with the 10 leaves.
   *
   * @return a new MerkleTreeInMemoryState with the 10 leaves.
   */
  public static MerkleTreeState newState() {
    MerkleTreeInMemoryState tree = MerkleTreeFixture.createInMemoryStateMerkleTree(10);
    return tree.getState();
  }

  /**
   * Creates n MerkleTreeInMemoryState with the 10 leaves.
   *
   * @param n the number of states to create.
   *
   * @return an array of n MerkleTreeInMemoryState with the 10 leaves.
   */
  public static ArrayList<MerkleTreeState> newStates(int n) {
    ArrayList<MerkleTreeState> states = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      states.add(MerkleTreeFixture.createInMemoryStateMerkleTree(10).getState());
    }
    return states;
  }
}
