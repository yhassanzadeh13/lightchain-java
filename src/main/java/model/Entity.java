package model;

import model.lightchain.Identifier;

/**
 * Entity represents the unit of data model in LightChain. Everything meant to be sent over the network, stored
 * in memory, or a database must extend the Entity class.
 */
public abstract class Entity {
  /**
   * Computes the collision resistant hash value of entity.
   *
   * @return identifier representation of hash value for entity.
   */
  public abstract Identifier id();
}

