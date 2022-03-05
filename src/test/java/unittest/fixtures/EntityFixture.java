package unittest.fixtures;

import model.Entity;
import model.lightchain.Identifier;

public class EntityFixture extends Entity {
  private static final String TYPE_FIXTURE_ENTITY = "fixture-entity-type";
  private final Identifier id;

  public EntityFixture() {
    super();
    this.id = IdentifierFixture.newIdentifier();
  }

  @Override
  public String type() {
    return TYPE_FIXTURE_ENTITY;
  }

  @Override
  public Identifier id() {
    return this.id;
  }
}
