package modules.ads.skiplist;

import java.util.ArrayList;
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
    while (this.currNode.getRight() != null && this.currNode.getRight().getIdentifier().comparedTo(id) < 0) {
      this.currNode = currNode.getRight();
      stk.push(this.currNode);
    }

  }

  public void dropDown() {
    if (this.currNode.getDown() != null) {
      this.currNode.setDropDown(true);
      this.currNode = this.currNode.getDown();
      stk.push(this.currNode);
    }
  }

  @Override
  public AuthenticatedEntity put(Entity e) {
    get(e.id());
    boolean coinFlip;
    do {
      SkipListNode currNodeStack = this.stk.pop();
      coinFlip = random.nextBoolean();
      if (coinFlip) {
        while (currNodeStack.isDropDown() && !this.stk.isEmpty()) {
          currNodeStack = this.stk.pop();
        }
      }
      if (currNodeStack == this.root) {
        currNodeStack.setTower(true);
      }
      SkipListNode downNode = null;
      if (currNodeStack.getDown() != null) {
        downNode = currNodeStack.getDown().getRight();
      }
      SkipListNode newNode = new SkipListNode(e.id(), currNodeStack.getRight(), downNode, coinFlip);
      currNodeStack.setRight(newNode);
      if (stk.isEmpty()) {
        this.root = new SkipListNode(this.root);
        newNode.setTower(false);
      }
      //currNodeStack.setDropDown(false);
    } while (coinFlip && !stk.isEmpty());

    return null;
  }

  @Override
  public AuthenticatedEntity get(Identifier id) {
    stk.clear();
    this.currNode = this.root;
    stk.push(this.currNode);
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
    this.currNode.setDropDown(true);
    ArrayList<SkipListNode> nodesInStack = new ArrayList(stk);
    System.out.println("Stack:");
    for (SkipListNode s : nodesInStack) {
      System.out.print(s.getIdentifier().toString().substring(0, 5));
      System.out.print(" ");
    }
    System.out.println();
    return null;
  }

  @Override
  public String toString() {
    int countX = 0;
    ArrayList<String> nodeArray = new ArrayList<String>();
    SkipListNode stringNode = this.root;
    while (stringNode.getDown() != null) {
      stringNode = stringNode.getDown();
    }
    nodeArray.add(stringNode.getIdentifier().toString().substring(0, 5));
    while (stringNode.getRight() != null) {
      countX += 1;
      stringNode = stringNode.getRight();
      nodeArray.add(stringNode.getIdentifier().toString().substring(0, 5));
    }

    System.out.print("\n");
    System.out.println("--- Skip List ---");

    StringBuilder prettyString = new StringBuilder();
    SkipListNode currNode = this.root;
    while (currNode != null) {
      int countY = 0;
      SkipListNode tempNode = currNode;
      String strId = currNode.getIdentifier().toString().substring(0, 5);
      prettyString.append(strId).append(" ");
      countY += 1;
      while (tempNode.getRight() != null) {
        tempNode = tempNode.getRight();
        strId = tempNode.getIdentifier().toString().substring(0, 5);
        int idx = nodeArray.indexOf(strId);
        for (int i = 0; i < ((idx - countY) * 6); i++) {
          prettyString.append(" ");
        }
        prettyString.append(strId).append(" ");
        countY += (idx - countY + 1);
      }
      prettyString.append("\n");
      currNode = currNode.getDown();
    }
    return prettyString.toString();
  }
}
