package model.lightchain;

/**
 * Represents list of assigned validators to a given entity.
 */
public class Assignment {
  /**
   * Identifier of validators.
   */
  private final Identifiers validators;

  public Assignment(Identifiers validators) {
    this.validators = validators;
  }
}
