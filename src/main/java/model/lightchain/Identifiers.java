package model.lightchain;

import java.util.ArrayList;

/**
 * Represents an aggregated type for identifiers.
 */
public class Identifiers {
  private final ArrayList<Identifier> identifiers;

  public Identifiers() {
    this.identifiers = new ArrayList<>();
  }

  public void add(Identifier identifier) {
    this.identifiers.add(identifier);
  }

  public boolean has(Identifier identifier) {
    return this.identifiers.contains(identifier);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof Identifiers)) {
      return false;
    }
    Identifiers that = (Identifiers) o;
    return this.identifiers.equals(that.identifiers);
  }

  @Override
  public int hashCode() {
    return this.identifiers.hashCode();
  }
}