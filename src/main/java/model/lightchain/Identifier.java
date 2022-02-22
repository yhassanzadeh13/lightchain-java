package model.lightchain;


public class Identifier {
  public static final int Size = 32;
  private final byte[] value;

  public Identifier(byte[] value) {
    this.value = value;
  }
}
