package model.lightchain;

import java.util.ArrayList;

/**
 * Represents an aggregated type for identifiers.
 */
public class Identifiers {
  private final ArrayList<Identifier> identifiers;

  public Identifiers(ArrayList<Identifier> identifiers) {
    this.identifiers = (ArrayList<Identifier>) identifiers.clone();
  }

  public void add(Identifier identifier) {
    this.identifiers.add(identifier);
  }

  public boolean has(Identifier identifier) {
    return this.identifiers.contains(identifier);
  }
}