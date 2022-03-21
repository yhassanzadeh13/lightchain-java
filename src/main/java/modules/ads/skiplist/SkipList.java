package modules.ads.skiplist;

import java.util.Random;
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
  private static final Random random = new Random();
  Stack<SkipListNode> stk = new Stack<>();
  private SkipListNode root;
  private SkipListNode currNode;

  public SkipList() {
    this.root = new SkipListNode();
    this.currNode = root;
  }

  public void hopForward(Identifier id) {

    while (this.currNode.getRight() != null && this.currNode.getRight().getIdentifier().comparedTo(id) > 0) {
      stk.push(this.currNode);
      System.out.println("Hop forward " + this.currNode.getIdentifier());
      this.currNode = currNode.getRight();
    }

  }

  public void dropDown() {
    stk.push(this.currNode);
    if (this.currNode.getDown() != null) {
      this.currNode = this.currNode.getDown();
    }
  }

  @Override
  public AuthenticatedEntity put(Entity e) {
    get(e.id());
    boolean coinFlip;
    do {
      SkipListNode currNodeStack = this.stk.pop();
      if (currNodeStack == this.root) {
        currNodeStack.setTower(true);
      }
      SkipListNode downNode = null;
      if (currNodeStack.getDown() != null) {
        downNode = currNodeStack.getRight();
      }
      coinFlip = random.nextBoolean();
      SkipListNode newNode = new SkipListNode(e.id(), currNodeStack.getRight(), downNode, coinFlip);
      currNodeStack.setRight(newNode);
    } while (coinFlip && !stk.isEmpty());
    if (stk.isEmpty()) {
      this.root = new SkipListNode(this.root);
    }
    return null;
  }

  @Override
  public AuthenticatedEntity get(Identifier id) {
    this.currNode = this.root;
    do {
      hopForward(id);
      dropDown();
    } while (this.currNode.getDown() != null);
    hopForward(id);
    if (id.comparedTo(this.currNode.getIdentifier()) == 0) {
      System.out.println("Found " + this.currNode.getIdentifier());
    } else {
      System.out.println("Not found " + id);
    }
    return null;
  }

  @Override
  public String toString() {
    StringBuilder prettyString = new StringBuilder();
    SkipListNode currNode = this.root;
    while (currNode != null) {
      SkipListNode tempNode = currNode;
      prettyString.append(tempNode.getIdentifier().toString()).append(" ");
      while (tempNode.getRight() != null) {
          tempNode = tempNode.getRight();
          prettyString.append(tempNode.getIdentifier().toString()).append(" ");
      }
      prettyString.append("\n");
      currNode = currNode.getDown();
    }
    return prettyString.toString();
  }
}
