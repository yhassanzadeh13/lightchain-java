package unittest.fixtures;

import model.lightchain.Identifier;
import modules.ads.merkletree.MerkleTreeInMemoryState;
import modules.ads.merkletree.MerkleTreeState;

import java.util.ArrayList;

/**
 * Encapsulates utilities for a merkle tree state.
 */
public class MerkleTreeStateFixture {
  public static MerkleTreeState newState() {
    MerkleTreeInMemoryState tree = MerkleTreeFixture.createMerkleTree(10);
    return tree.getState();
  }

  public static ArrayList<MerkleTreeState> newStates(int n) {
    ArrayList<MerkleTreeState> states = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      states.add(MerkleTreeFixture.createMerkleTree(10).getState());
    }
    return states;
  }
}
