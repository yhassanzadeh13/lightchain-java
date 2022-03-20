package modules.ads.skiplist;

import crypto.Sha3256Hasher;
import model.Entity;
import model.codec.EncodedEntity;
import model.crypto.Sha3256Hash;
import modules.codec.JsonEncoder;

public class Node {
  private Entity e;
  private Node right;
  private Node down;
  private boolean isTower;
  private Sha3256Hash FV;
  private static final Sha3256Hasher hasher = new Sha3256Hasher();
  private static final JsonEncoder encoder = new JsonEncoder();

  public Node(Entity e, Node right, Node down, boolean isTower) {
    this.e = e;
    this.right = right;
    this.down = down;
    this.isTower = isTower;
  }

  public void calculateFV() {
    EncodedEntity encodedEntityRight = encoder.encode(right.getE());
    EncodedEntity encodedEntityDown = encoder.encode(down.getE());
    if (this.down == null) {
      if (this.isTower) {
        this.FV = hasher.computeHash(encodedEntityRight, encodedEntityDown);
      } else {
        this.FV = hasher.computeHash(encodedEntityRight, right.getFV());
      }
    } else {
      if (this.isTower) {
        this.FV = down.getFV();
      } else {
        this.FV = hasher.computeHash(down.getFV(), right.getFV());
      }
    }
  }

  public Sha3256Hash getFV() {
    return FV;
  }

  public void setFV(Sha3256Hash FV) {
    this.FV = FV;
  }

  public Entity getE() {
    return e;
  }

  public void setE(Entity e) {
    this.e = e;
  }

  public Node getRight() {
    return right;
  }

  public void setRight(Node right) {
    this.right = right;
  }

  public Node getDown() {
    return down;
  }

  public void setDown(Node down) {
    this.down = down;
  }

  public boolean isTower() {
    return isTower;
  }

  public void setTower(boolean tower) {
    isTower = tower;
  }
}
