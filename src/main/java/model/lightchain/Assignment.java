package model.lightchain;

/**
 * Represents list of assigned validators to a given entity.
 */
public class Assignment {
  /**
   * Identifier of validators.
   */
  private Identifiers validators;

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
}
