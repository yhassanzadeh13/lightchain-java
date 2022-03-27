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
  private final Stack<SkipListNode> searchStack = new Stack<>();
  private final Stack<SkipListNode> updateStack = new Stack<>();
  private final Stack<SkipListNode> insertionStack = new Stack<>();
  private SkipListNode root;
  private SkipListNode currNode;

  public SkipList() {
    this.root = new SkipListNode();
    this.currNode = root;
  }

  public void hopForward(Identifier id) {
    while (this.currNode.getRight() != null && this.currNode.getRight().getIdentifier().comparedTo(id) <= 0) {
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
    if (e == null) {
      return null;
    }
    get(e);
    updateStack.addAll(searchStack);
    if (e.id().comparedTo(this.currNode.getIdentifier()) != 0) {
      boolean coinFlip;
      do {
        SkipListNode newNode = new SkipListNode(e);
        coinFlip = random.nextBoolean();
        if (searchStack.size() > 0) {
          SkipListNode currNodeStack = this.searchStack.pop();
          if (insertionStack.isEmpty()) { // if the node is in the base level
            newNode.setRight(currNodeStack.getRight()); // change in right
            newNode.calculateFV();
            currNodeStack.setRight(newNode); // change in right
            currNodeStack.setDropDown(false); // change in dropDown
          } else { // if the node is in a higher level
            SkipListNode oldNode = insertionStack.pop();
            oldNode.setTower(true); // change in tower
            newNode.setDown(oldNode); // change in down
            newNode.calculateFV();
            while (!this.searchStack.isEmpty() && !currNodeStack.isDropDown()) {
              currNodeStack = this.searchStack.pop();
            }
            if (currNodeStack.isDropDown()) {
              newNode.setRight(currNodeStack.getRight()); // change in right
              newNode.calculateFV();
              currNodeStack.setRight(newNode); // change in right
              currNodeStack.setDropDown(false); // change in dropDown
            } else {
              SkipListNode newRoot = new SkipListNode();
              this.root.setTower(true); // change in tower
              newRoot.setDown(this.root); // change in down
              newRoot.setRight(newNode); // change in right
              newRoot.calculateFV();
              this.root = newRoot; // change in root
            }
          }
        } else {
          if (this.root.getDown() == null && this.root.getRight() == null) { // first insertion
            this.root.setRight(newNode); // change in right
            this.root.calculateFV(); // update FV
          } else {
            SkipListNode newRoot = new SkipListNode();
            this.root.setTower(true);
            this.root.calculateFV(); // update FV
            newRoot.setDown(this.root); // change in down
            newRoot.setRight(newNode); // change in right
            newRoot.calculateFV(); // update FV
            this.root = newRoot; // change in root
            SkipListNode oldNode = insertionStack.pop();
            oldNode.setTower(true); // change in tower
            oldNode.calculateFV(); // update FV
            newNode.setDown(oldNode); // change in down
            newNode.calculateFV(); // update FV
          }
        }
        insertionStack.push(newNode);
      } while (coinFlip);
      for (SkipListNode node : updateStack) {
        node.calculateFV();
      }
      insertionStack.clear();
      updateStack.clear();
    }
    return null;
  }

  @Override
  public AuthenticatedEntity get(Entity e) {
    Identifier id = e.id();
    searchStack.clear();
    this.currNode = this.root;
    this.currNode.setDropDown(false);
    searchStack.push(this.currNode);
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
    if (!searchStack.isEmpty()) {

      Proof proof = new Proof((Stack<SkipListNode>) searchStack.clone(), this.root.getIdentifier());
      return new modules.ads.skiplist.AuthenticatedEntity(proof, e.type(), e);

    } else {
      return null;
    }
  }

  @Override
  public String toString() {
    int countX = 0;
    ArrayList<String> nodeArray = new ArrayList<>();
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
