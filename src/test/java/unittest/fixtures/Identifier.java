package unittest.fixtures;

public class Identifier {
  public static model.lightchain.Identifier IdentifierFixture() {
    byte[] bytes = Bytes.ByteArrayFixture(model.lightchain.Identifier.Size);
    return new model.lightchain.Identifier(bytes);
  }
}
