package bootstrap;

import model.Entity;
import model.lightchain.Identifier;

/**
 * Special Class to enable the transmission of Strings containing the HelloMessages via P2pNetwork Entities.
 */
public class HelloMessageEntity extends Entity {
  private final String content;
  private final Identifier senderId;

  /**
   * Constructor for the HelloMessageEntity.
   *
   * @param content  The content of the HelloMessage.
   * @param senderId The id of the sender of the HelloMessage.
   */
  public HelloMessageEntity(String content, Identifier senderId) {
    super();
    this.content = content;
    this.senderId = senderId;
  }

  /**
   * Returns the type of the HelloMessageEntity.
   *
   * @return The type of the HelloMessageEntity.
   */
  @Override
  public String type() {
    return "HelloMessageEntity";
  }

  public String getContent() {
    return content;
  }

  public Identifier getSenderId() {
    return senderId;
  }
}
