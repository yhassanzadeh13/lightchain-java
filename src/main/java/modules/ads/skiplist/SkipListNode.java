package modules.ads.skiplist;

import crypto.Sha3256Hasher;
import model.Entity;
import model.crypto.Sha3256Hash;
import model.lightchain.Identifier;
import modules.codec.JsonEncoder;

public class SkipListNode {
  private static final Sha3256Hasher hasher = new Sha3256Hasher();
  private static final JsonEncoder encoder = new JsonEncoder();
  private Identifier identifier;
  private Entity entity;
  private SkipListNode right;
  private SkipListNode down;
  private boolean isTower;
  private Sha3256Hash FV;
  private boolean isDropDown;

  public SkipListNode() {
    this.entity = null;
    this.identifier = new Identifier(new byte[32]);
    this.right = null;
    this.down = null;
    this.isTower = false;
    this.isDropDown = false;
    calculateFV();
  }

  public SkipListNode(Entity e) {
    this.entity = e;
    this.identifier = e.id();
    this.right = null;
    this.down = null;
    this.isTower = false;
    this.isDropDown = false;
    calculateFV();
  }

  public void calculateFV() { // v is curr node, u is down of curr node, w is right of curr node
    if (this.right == null) { // f(v) = 0
      byte[] zeroHashBytes = new byte[32];
      this.FV = new Sha3256Hash(zeroHashBytes);
    } else if (this.down == null) {
      if (this.isTower) { // f(v) = h(elem(v),elem(w))
        this.FV = hasher.computeHash(this.identifier.getBytes(), this.right.getIdentifier().getBytes());
      } else { // f(v) = h(elem(v), f(w))
        this.FV = hasher.computeHash(this.identifier.getBytes(), this.right.getFV().getHashBytes());
      }
    } else {
      if (this.isTower) { // f(v) = f(u).
        this.FV = this.down.getFV();
      } else { // f(v) = h(f(u), f(w))
        this.FV = hasher.computeHash(this.down.getFV().getHashBytes(), this.right.getFV().getHashBytes());
      }
    }
  }

  public Entity getEntity() {
    return entity;
  }

  public void setEntity(Entity entity) {
    this.entity = entity;
  }

  public boolean isDropDown() {
    return isDropDown;
  }

  public void setDropDown(boolean dropDown) {
    this.isDropDown = dropDown;
  }

  public Sha3256Hash getFV() {
    return FV;
  }

  public void setFV(Sha3256Hash FV) {
    this.FV = FV;
  }

  public Identifier getIdentifier() {
    return identifier;
  }

  public void setIdentifier(Identifier identifier) {
    this.identifier = identifier;
  }

  public SkipListNode getRight() {
    return right;
  }

  public void setRight(SkipListNode right) {
    this.right = right;
  }

  public SkipListNode getDown() {
    return down;
  }

  public void setDown(SkipListNode down) {
    this.down = down;
  }

  public boolean isTower() {
    return isTower;
  }

  public void setTower(boolean tower) {
    isTower = tower;
  }
}
