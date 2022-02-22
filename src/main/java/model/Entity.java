package model;

import java.io.Serializable;

import model.crypto.Hash;

/**
 * Entity represents the unit of data model in LightChain. Everything meant to be sent over the network, stored
 * in memory, or a database must extend the Entity class.
 */
abstract class Entity {
  /**
   * Computes the collision resistant hash value of entity in bytes.
   * @return hash object for entity.
   */
  abstract Hash Hash();
}

