package modules.ads.skiplist;

import java.util.Stack;

import model.lightchain.Identifier;
import modules.ads.MembershipProof;

public class Proof implements MembershipProof {
  private Stack<SkipListNode> path;
  private Identifier root;

  public Proof(Stack<SkipListNode> path, Identifier root) {
    this.path = path;
    this.root = root;
  }

  /**
   * Root of the authenticated data structure that this proof belongs to.
   *
   * @return root identifier.
   */
  @Override
  public Identifier getRoot() {
    return this.root;
  }

  /**
   * Sets the root of the authenticated data structure that this proof belongs to.
   *
   * @param root identifier of the root.
   */
  public void setRoot(Identifier root) {
    this.root = root;
  }

  /**
   * Returns the path of the proof of membership.
   *
   * @param identifier identifier of the node to be verified.
   * @return path of the proof of membership.
   */
  @Override
  public Stack<SkipListNode> getPath(Identifier identifier) {
    return this.path;
  }

  /**
   * Sets the path of the proof of membership.
   *
   * @param path path of the proof of membership.
   */
  public void setPath(Stack<SkipListNode> path) {
    this.path = path;
  }
}
