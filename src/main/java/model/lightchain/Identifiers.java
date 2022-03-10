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
}