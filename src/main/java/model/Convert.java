package model;

import java.util.ArrayList;

import model.lightchain.Identifiers;

/**
 * Convert encapsulates conversion methods for converting one model into another model.
 */
public class Convert {
  /**
   * Converts entities to list of their identifiers.
   *
   * @param entities array of entities.
   * @return list of their identifiers.
   */
  public static Identifiers IdentifierOf(Entity[] entities) {
    Identifiers ids = new Identifiers();

    for (Entity e : entities) {
      ids.add(e.id());
    }

    return ids;
  }

  /**
   * Converts entities to list of their identifiers.
   *
   * @param entities array of entities.
   * @return list of their identifiers.
   */
  public static Identifiers IdentifierOf(ArrayList<Entity> entities) {
    Identifiers ids = new Identifiers();

    for (Entity e : entities) {
      ids.add(e.id());
    }

    return ids;
  }
}
