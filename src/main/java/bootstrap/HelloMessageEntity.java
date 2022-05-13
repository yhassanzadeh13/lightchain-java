package bootstrap;

import model.Entity;

/**
 * Special Class to enable the transmission of Strings containing the HelloMessages via P2pNetwork Entities.
 */
public class HelloMessageEntity extends Entity {
  public String content;

  /**
   * Constructor for the HelloMessageEntity.
   *
   * @param content The content of the HelloMessage.
   */
  public HelloMessageEntity(String content) {
    super();
    this.content = content;
  }

  /**
   * Returns the type of the HelloMessageEntity.
   *
   * @return The type of the HelloMessageEntity.
   */
  @Override
  public String type() {
    return "String";
  }
}