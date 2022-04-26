package modules.ads;

import model.Entity;
import model.lightchain.Identifier;
import modules.ads.merkletree.MerkleProof;

public interface MerkleTree {
  default modules.ads.AuthenticatedEntity put(Entity e) throws IllegalArgumentException{
    return null;
  }
  default modules.ads.AuthenticatedEntity get(Identifier id) throws IllegalArgumentException{
    return null;
  }
  default MerkleProof getProof(Identifier id) throws IllegalArgumentException{
    return null;
  }
  void buildMerkleTree();
  int size();
  byte[] getBytes();
}
