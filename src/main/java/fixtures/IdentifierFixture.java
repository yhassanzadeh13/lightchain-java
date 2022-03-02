package fixtures;

public class IdentifierFixture {
  public static model.lightchain.Identifier NewIdentifier() {
    byte[] bytes = Bytes.ByteArrayFixture(model.lightchain.Identifier.Size);
    return new model.lightchain.Identifier(bytes);
  }
}
