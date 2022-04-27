package unittest.fixtures;

import java.util.ArrayList;

import modules.ads.merkletree.MerkleTreeInMemoryState;
import modules.ads.merkletree.MerkleTreeState;

/**
 * Encapsulates utilities for a merkle tree state.
 */
public class MerkleTreeStateFixture {
  public static MerkleTreeState newState() {
    MerkleTreeInMemoryState tree = MerkleTreeFixture.createInMemoryStateMerkleTree(10);
    return tree.getState();
  }

  public static ArrayList<MerkleTreeState> newStates(int n) {
    ArrayList<MerkleTreeState> states = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      states.add(MerkleTreeFixture.createInMemoryStateMerkleTree(10).getState());
    }
    return states;
  }
}
