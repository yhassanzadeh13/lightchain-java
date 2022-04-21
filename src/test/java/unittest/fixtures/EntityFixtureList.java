package unittest.fixtures;

import java.util.ArrayList;

import model.Entity;

/**
 * Creates list of fixture entities.
 */
public class EntityFixtureList {

  /**
   * Creates list of fixture entities.
   *
   * @param count total number of fixture entities.
   * @return list of fixture entities.
   */
  public static ArrayList<Entity> newList(int count) {
    ArrayList<Entity> entities = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      entities.add(new EntityFixture());
    }
    return entities;
  }
}
