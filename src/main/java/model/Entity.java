package model;

import crypto.Sha3256Hasher;
import model.codec.EncodedEntity;
import model.crypto.Hash;
import model.lightchain.Identifier;
import modules.codec.JsonEncoder;

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
  public Identifier id() {
    JsonEncoder c = new JsonEncoder();
    EncodedEntity e = c.encode(this);
    Sha3256Hasher h = new Sha3256Hasher();
    Hash hash = h.computeHash(e);
    return hash.toIdentifier();
  }

  /**
   * Type of this entity.
   *
   * @return type of this entity.
   */
  public abstract String Type();
}

