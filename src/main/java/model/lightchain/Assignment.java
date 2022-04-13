package model.lightchain;

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

  /**
   * Checks whether an identifier is assigned to this assignment.
   *
   * @param validator
   * @return true if validator is in assignment
   */
  public boolean has(Identifier validator) {
    return this.validators.has(validator);
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
}
