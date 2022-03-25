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
  private static final Identifier baseIdentifier = new Identifier(new byte[32]);
  Stack<SkipListNode> searchStack = new Stack<>();
  Stack<SkipListNode> insertionStack = new Stack<>();
  private SkipListNode root;
  private SkipListNode currNode;

  public SkipList() {
    this.root = new SkipListNode();
    this.currNode = root;
  }

  public void hopForward(Identifier id) {
    while (this.currNode.getRight() != null && this.currNode.getRight().getIdentifier().comparedTo(id) < 0) {
      this.currNode = currNode.getRight();
      this.currNode.setDropDown(false);
      searchStack.push(this.currNode);
    }

  }

  public void dropDown() {
    if (this.currNode.getDown() != null) {
      this.currNode.setDropDown(true);
      this.currNode = this.currNode.getDown();
      searchStack.push(this.currNode);
    }
  }

  @Override
  public AuthenticatedEntity put(Entity e) {
    get(e.id());
    boolean coinFlip;
    do {
      SkipListNode newNode = new SkipListNode(e.id());
      coinFlip = random.nextBoolean();
      if (searchStack.size() > 0) {
        SkipListNode currNodeStack = this.searchStack.pop();
        if (insertionStack.isEmpty()) { // if the node is in the base level
          newNode.setRight(currNodeStack.getRight());
          currNodeStack.setRight(newNode);
          currNodeStack.setDropDown(false);
        } else { // if the node is in a higher level
          SkipListNode oldNode = insertionStack.pop();
          oldNode.setTower(true);
          newNode.setDown(oldNode);
          while (!this.searchStack.isEmpty() && !currNodeStack.isDropDown()) {
            currNodeStack = this.searchStack.pop();
          }
          if (currNodeStack.isDropDown()) {
            newNode.setRight(currNodeStack.getRight());
            currNodeStack.setRight(newNode);
            currNodeStack.setDropDown(false);
          } else {
            SkipListNode newRoot = new SkipListNode();
            this.root.setTower(true);
            newRoot.setDown(this.root);
            newRoot.setRight(newNode);
            this.root = newRoot;
          }
        }
      } else {
        if (this.root.getDown() == null && this.root.getRight() == null) {
          this.root.setRight(newNode);
        } else {
          SkipListNode newRoot = new SkipListNode();
          this.root.setTower(true);
          newRoot.setDown(this.root);
          newRoot.setRight(newNode);
          this.root = newRoot;
          SkipListNode oldNode = insertionStack.pop();
          oldNode.setTower(true);
          newNode.setDown(oldNode);
        }
      }
      insertionStack.push(newNode);
    } while (coinFlip);
    insertionStack.clear();
    return null;
  }

  @Override
  public AuthenticatedEntity get(Identifier id) {
    searchStack.clear();
    this.currNode = this.root;
    this.currNode.setDropDown(false);
    searchStack.push(this.currNode);
    do {
      hopForward(id);
      dropDown();
    } while (this.currNode.getDown() != null);
    hopForward(id);
    /*
    if (id.comparedTo(this.currNode.getIdentifier()) == 0) {
      System.out.println("Found " + this.currNode.getIdentifier());
    } else {
      System.out.println("Not found " + id);
    }
     */
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

    //System.out.print("\n");
    //System.out.println("--- Skip List ---");

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
