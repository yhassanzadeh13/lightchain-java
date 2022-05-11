package model;

import java.io.Serializable;

import crypto.Sha3256Hasher;
import model.codec.EncodedEntity;
import model.crypto.Hash;
import model.exceptions.CodecException;
import model.lightchain.Identifier;
import modules.codec.JsonEncoder;

/**
 * Entity represents the unit of data model in LightChain. Everything meant to be sent over the network, stored
 * in memory, or a database must extend the Entity class.
 */
public abstract class Entity implements Serializable {
  /**
   * Computes the collision resistant hash value of entity.
   *
   * @return identifier representation of hash value for entity.
   */
  public Identifier id() {
    JsonEncoder c = new JsonEncoder();
    EncodedEntity e = null;
    try {
      e = c.encode(this);
    } catch (CodecException ex) {
      System.err.println(ex.getMessage());
      System.exit(1);
    }
    Sha3256Hasher h = new Sha3256Hasher();
    Hash hash = h.computeHash(e);
    return hash.toIdentifier();
  }

  /**
   * Type of this entity.
   *
   * @return type of this entity.
   */
  public abstract String type();
}

