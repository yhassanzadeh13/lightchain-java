package unittest.fixtures;

import java.util.Stack;

import modules.ads.skiplist.SkipList;
import modules.ads.skiplist.SkipListNode;

public class SkipListFixture extends SkipList {
  Stack<SkipListNode> stk = new Stack<>();
  private SkipListNode root;
  private SkipListNode currNode;

  public SkipListFixture() {
    super();
  }
}
