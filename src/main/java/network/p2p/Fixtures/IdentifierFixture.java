package network.p2p.Fixtures;

/**
 * Encapsulates utilities for a LightChain identifier.
 */
public class IdentifierFixture {
  public static model.lightchain.Identifier newIdentifier() {
    byte[] bytes = Bytes.byteArrayFixture(model.lightchain.Identifier.Size);
    return new model.lightchain.Identifier(bytes);
  }
}
