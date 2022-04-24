package storage;

import java.util.ArrayList;

import modules.ads.merkletree.MerkleNode;

public interface MerkleNodes {
  boolean add(MerkleNode node);
  boolean has(MerkleNode node);
  boolean remove(MerkleNode node);
  ArrayList<MerkleNode> all();
}
