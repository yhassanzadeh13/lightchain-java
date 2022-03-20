package modules.ads.skiplist;

import model.Entity;
import model.lightchain.Identifier;
import modules.ads.AuthenticatedDataStructure;
import modules.ads.AuthenticatedEntity;

/**
 * Implementation of an in-memory Authenticated Skip List
 * that is capable of storing and retrieval of LightChain entities.
 */
public class SkipList implements AuthenticatedDataStructure {
  private SkipListNode root;
  private SkipListNode currNode;

  public SkipList(SkipListNode root) {
    this.root = root;
    this.currNode = root;
  }

  @Override
  public AuthenticatedEntity put(Entity e) {
    return null;
  }

  @Override
  public AuthenticatedEntity get(Identifier id) {

    return null;
  }
}
