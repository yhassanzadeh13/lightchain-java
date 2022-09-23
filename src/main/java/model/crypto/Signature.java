package model.crypto;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

import model.Entity;
import model.lightchain.Identifier;

/**
 * Represents abstract data type for the cryptographic digital signature used in LightChain.
 */
public abstract class Signature extends Entity implements Serializable {
  /**
   * The signature value in bytes.
   */
  private final byte[] bytes;
  /**
   * Identifier of node that signed transaction.
   */
  private final Identifier signerId;

  public Signature(byte[] bytes, Identifier signerId) {
    this.bytes = bytes.clone();
    this.signerId = signerId;
  }

  public Identifier getSignerId() {
    return signerId;
  }

  public byte[] getBytes() {
    return bytes.clone();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Signature)) return false;
    Signature signature = (Signature) o;
    return Arrays.equals(getBytes(), signature.getBytes()) && Objects.equals(getSignerId(), signature.getSignerId());
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(getSignerId());
    result = 31 * result + Arrays.hashCode(getBytes());
    return result;
  }
}
