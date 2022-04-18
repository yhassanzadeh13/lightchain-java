package bootstrap;

import model.Entity;

public class HelloMessageEntity extends Entity {

  public String content;

  public HelloMessageEntity(String content) {
    super();
    this.content=content;
  }

  @Override
  public String type() {
    return "String";
  }
}
