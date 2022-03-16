package storage;

import model.lightchain.Identifier;

public interface Identifiers {
  boolean Add(Identifier identifier);
  boolean Has(Identifier identifier);
}
