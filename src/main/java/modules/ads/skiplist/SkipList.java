package modules.ads.skiplist;

import java.util.Stack;

import model.Entity;
import model.lightchain.Identifier;
import modules.ads.AuthenticatedDataStructure;
import modules.ads.AuthenticatedEntity;

/**
 * Implementation of an in-memory Authenticated Skip List
 * that is capable of storing and retrieval of LightChain entities.
 */
public class SkipList implements AuthenticatedDataStructure {
  Stack<SkipListNode> stk = new Stack<>();
  private SkipListNode root;
  private SkipListNode currNode;

  public SkipList() {
    this.root = new SkipListNode();
    this.currNode = root;
  }

  public void hopForward(Identifier id) {
    if (this.currNode.getRight() != null) {
      while (this.currNode.getRight().getIdentifier().comparedTo(id) < 0) {
        stk.push(this.currNode);
        this.currNode = currNode.getRight();
      }
    }
  }

  public void dropDown() {
    if (this.currNode.getDown() != null) {
      this.currNode = this.currNode.getDown();
    }
  }

  @Override
  public AuthenticatedEntity put(Entity e) {
    return null;
  }

  @Override
  public AuthenticatedEntity get(Identifier id) {
    while (this.currNode.getDown() != null) {
      hopForward(id);
      dropDown();
    }
    if (id.comparedTo(this.currNode.getIdentifier()) == 0) {
      System.out.println("Found " + this.currNode.getIdentifier());
    } else {
      System.out.println("Not found " + id);
    }
    return null;
  }
}
