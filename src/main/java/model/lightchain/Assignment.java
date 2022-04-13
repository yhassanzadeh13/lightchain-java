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

  /**
   * Checks whether an identifier is assigned to this assignment.
   *
   * @param validator
   * @return true if validator is in assignment
   */
  public boolean has(Identifier validator) {
    return this.validators.has(validator);
  }
}
