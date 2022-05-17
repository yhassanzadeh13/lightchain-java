package model.lightchain;

import java.io.Serializable;
import java.util.Arrays;

import io.ipfs.multibase.Multibase;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a 32-byte unique identifier for an entity. Normally is computed as the hash value of the entity.
 */
public class Identifier implements Serializable,Comparable<Identifier> {
  public static final int Size = 32;
  private final byte[] value;

  public Identifier(byte[] value) {
    this.value = value.clone();
  }

  /**
   * Returns if objects equal.
   *
   * @param o an identifier object.
   * @return true if objcets equal.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Identifier that = (Identifier) o;
    return Arrays.equals(value, that.value);
  }

  /**
   * Return the hashCode.
   *
   * @return hashCode.
   */
  @Override
  public int hashCode() {
    return Arrays.hashCode(value);
  }

  public byte[] getBytes() {
    return this.value.clone();
  }

  /**
   * Returns string representation of identifier in Base58BTC.
   *
   * @return string representation of identifier in Base58BTC.
   */
  public String toString() {
    return pretty(this.value);
  }

  /**
   * Converts identifier from its byte representation to Base58BTC.
   *
   * @param identifier input identifier in byte representation.
   * @return Base58BTC representation of identifier.
   */
  private static String pretty(byte[] identifier) {
    return Multibase.encode(Multibase.Base.Base58BTC, identifier);
  }

  /**
   * Compares this identifier with the other identifier.
   *
   * @param other represents other identifier to compared to.
   * @return 0 if two identifiers are equal, 1 if this identifier is greater than other,
   * -1 if other identifier is greater than this.
   */
  public int comparedTo(Identifier other) {
    int result = Arrays.compare(this.value, other.value);
    return Integer.compare(result, 0);
  }

  /**
   * Compares this object with the specified object for order.  Returns a
   * negative integer, zero, or a positive integer as this object is less
   * than, equal to, or greater than the specified object.
   *
   * <p>The implementor must ensure {@link Integer#signum
   * signum}{@code (x.compareTo(y)) == -signum(y.compareTo(x))} for
   * all {@code x} and {@code y}.  (This implies that {@code
   * x.compareTo(y)} must throw an exception if and only if {@code
   * y.compareTo(x)} throws an exception.)
   *
   * <p>The implementor must also ensure that the relation is transitive:
   * {@code (x.compareTo(y) > 0 && y.compareTo(z) > 0)} implies
   * {@code x.compareTo(z) > 0}.
   *
   * <p>Finally, the implementor must ensure that {@code
   * x.compareTo(y)==0} implies that {@code signum(x.compareTo(z))
   * == signum(y.compareTo(z))}, for all {@code z}.
   *
   * @param o the object to be compared.
   * @return a negative integer, zero, or a positive integer as this object
   * is less than, equal to, or greater than the specified object.
   * @throws NullPointerException if the specified object is null
   * @throws ClassCastException   if the specified object's type prevents it
   *                              from being compared to this object.
   * @apiNote It is strongly recommended, but <i>not</i> strictly required that
   * {@code (x.compareTo(y)==0) == (x.equals(y))}.  Generally speaking, any
   * class that implements the {@code Comparable} interface and violates
   * this condition should clearly indicate this fact.  The recommended
   * language is "Note: this class has a natural ordering that is
   * inconsistent with equals."
   */
  @Override
  public int compareTo(@NotNull Identifier o) {
    int result = Arrays.compare(this.value, o.value);
    return Integer.compare(result, 0);
  }
}
