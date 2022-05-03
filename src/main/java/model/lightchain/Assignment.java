package model.lightchain;

import java.util.ArrayList;

/**
 * Represents list of assigned validators to a given entity.
 */
public class Assignment {
  /**
   * Identifier of validators.
   */
  private final Identifiers validators;

  /**
   * Default constructor.
   */
  public Assignment() {
    this.validators = new Identifiers();
  }

  /**
   * Adds validator to assignment.
   *
   * @param validator identifier of validator.
   */
  public void add(Identifier validator) {
    this.validators.add(validator);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Assignment that = (Assignment) o;
    return validators.equals(that.validators);
  }

  @Override
  public int hashCode() {
    return validators.hashCode();
  }

  public int size() {
    return this.validators.size();
  }

  @Override
  public String toString() {
    return this.validators.toString();
  }

  public boolean has(Identifier id) {return this.validators.has(id);}

  public ArrayList<Identifier> all() {
    return this.validators.all();
  }
}
