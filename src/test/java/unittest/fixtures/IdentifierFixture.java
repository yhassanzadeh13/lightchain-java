package unittest.fixtures;

public class IdentifierFixture {
  public static model.lightchain.Identifier newIdentifier() {
    byte[] bytes = Bytes.byteArrayFixture(model.lightchain.Identifier.Size);
    return new model.lightchain.Identifier(bytes);
  }
}
