package storage;

import java.util.ArrayList;

import modules.ads.merkletree.MerkleTree;

public interface MerkleTrees {
  boolean add(MerkleTree tree);
  boolean has(MerkleTree tree);
  boolean remove(MerkleTree tree);
  ArrayList<MerkleTree> all();
}
