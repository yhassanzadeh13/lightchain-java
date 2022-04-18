package bootstrap;

import model.Entity;

/**
 * Special Class to enable the transmission of Strings containing the HelloMessages via P2pNetwork Entities.
 */
public class HelloMessageEntity extends Entity {

  public String content;

  public HelloMessageEntity(String content) {
    super();
    this.content = content;
  }

  @Override
  public String type() {
    return "String";
  }
}
