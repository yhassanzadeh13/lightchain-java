package unittest.fixtures;

import modules.ads.skiplist.SkipList;


public class SkipListFixture {
  /**
   * Creates a new skip list with n random elements.
   *
   * @param n number of elements to create
   * @return a new skip list with n random elements
   */
  public static SkipList createSkipList(int n) {
    SkipList skipList = new SkipList();
    for (int i = 0; i < n; i++) {
      skipList.put(new EntityFixture());
    }
    return skipList;
  }
}

