package modules.ads.skiplist;

import crypto.Sha3256Hasher;
import model.crypto.Sha3256Hash;
import model.lightchain.Identifier;
import modules.codec.JsonEncoder;

public class SkipListNode {
  private static final Sha3256Hasher hasher = new Sha3256Hasher();
  private static final JsonEncoder encoder = new JsonEncoder();
  private Identifier identifier;
  private SkipListNode right;
  private SkipListNode down;
  private boolean isTower;
  private Sha3256Hash FV;


  public SkipListNode(Identifier identifier, SkipListNode right, SkipListNode down, boolean isTower) {
    this.identifier = identifier;
    this.right = right;
    this.down = down;
    this.isTower = isTower;
    calculateFV();
  }

  public SkipListNode() {
    this.identifier = new Identifier(new byte[32]);
    this.right = null;
    this.down = null;
    this.isTower = true;
    calculateFV();
  }

  public void calculateFV() {
    if (this.right == null) {
      byte[] zeroHashBytes = new byte[32];
      this.FV = new Sha3256Hash(zeroHashBytes);
    } else if (this.down == null) {
      if (this.isTower) {
        this.FV = hasher.computeHash(this.identifier, right.getIdentifier());
      } else {
        this.FV = hasher.computeHash(this.identifier, right.getFV());
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
