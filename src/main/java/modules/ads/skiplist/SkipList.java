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
    get(e); // this updates the search stack
    updateStack.addAll(searchStack);
    if (e.id().comparedTo(this.currNode.getIdentifier()) != 0) {
      boolean coinFlip;
      do {
        SkipListNode newNode = new SkipListNode(e);
        coinFlip = random.nextBoolean();
        if (searchStack.size() > 0) { //
          SkipListNode currNodeStack = this.searchStack.pop();
          if (insertionStack.isEmpty()) { // if the node is in the base level
            newNode.setRight(currNodeStack.getRight());
            newNode.calculateFV();
            currNodeStack.setRight(newNode);
            currNodeStack.setDropDown(false);
          } else { // if the node is in a higher level
            SkipListNode oldNode = insertionStack.pop();
            oldNode.setTower(true);
            newNode.setDown(oldNode);
            newNode.calculateFV();
            while (!this.searchStack.isEmpty() && !currNodeStack.isDropDown()) {
              currNodeStack = this.searchStack.pop();
            }
            if (currNodeStack.isDropDown()) {
              newNode.setRight(currNodeStack.getRight());
              newNode.calculateFV();
              currNodeStack.setRight(newNode);
              currNodeStack.setDropDown(false);
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
            this.root.setRight(newNode);
            this.root.calculateFV();
          } else { // creates another level when coinFlip is n+1 times true, where n is the number of levels
            SkipListNode newRoot = new SkipListNode();
            this.root.setTower(true);
            this.root.calculateFV();
            newRoot.setDown(this.root);
            newRoot.setRight(newNode);
            newRoot.calculateFV();
            this.root = newRoot;
            SkipListNode oldNode = insertionStack.pop();
            oldNode.setTower(true);
            oldNode.calculateFV();
            newNode.setDown(oldNode);
            newNode.calculateFV();
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
    return get(e);
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
    ArrayList<byte[]> path = getSequence();
    Proof proof;
    if (id.comparedTo(this.currNode.getIdentifier()) == 0) {
      proof = new Proof(path, this.root.getFV(), true);
    } else {
      return null;
    }
    return new modules.ads.skiplist.AuthenticatedEntity(proof, e.type(), e);
  }

  private ArrayList<byte[]> getSequence() {
    ArrayList<SkipListNode> pSequence = new ArrayList<>(searchStack);
    //Collections.reverse(pSequence);
    ArrayList<byte[]> qSequence = new ArrayList<>();
    SkipListNode w1 = pSequence.get(0).getRight(); // w1 ← right(v1)
    if (w1 == null) { // if v is one of the right most nodes
      qSequence.add(new byte[32]);
    } else if (w1.isTower()) {
      qSequence.add(w1.getIdentifier().getBytes()); // x0 ← f(w1)
    } else { // if w1 is a plateau node
      qSequence.add(w1.getFV().getHashBytes()); // x0 ← elem(w1)
    }
    qSequence.add(pSequence.get(0).getIdentifier().getBytes()); // x1 ← x, where x is the searched node
    for (int i = 1; i < pSequence.size() - 1; i++) { // for i ← 2,···,m−1
      SkipListNode currNode = pSequence.get(i);
      SkipListNode currW = currNode.getRight(); // wi ← right(vi), where wi is the right node of vi
      if (currW == null) {
        qSequence.add(new byte[32]);
      } else if (!currW.isTower()) { // if wi is a plateau node
        if (currW != pSequence.get(i - 1)) { // if wi != vi−1
          qSequence.add(currW.getFV().getHashBytes()); // xj ← f(wi), paper uses j for numbering issues. i is enough here.
        } else { // if wi = vi−1
          if (currNode.getDown() == null) { // if vi is in the base list S0
            qSequence.add(currNode.getIdentifier().getBytes()); // xj ← elem(vi)
          } else {
            qSequence.add(currNode.getDown().getFV().getHashBytes()); // xj ← f(ui), where ui is the down node of vi
          }
        }
      }
    }
    return qSequence;
  }
}
