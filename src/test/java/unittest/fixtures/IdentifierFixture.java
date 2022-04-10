package unittest.fixtures;

import java.util.ArrayList;

import model.lightchain.Identifier;

/**
 * Encapsulates utilities for a LightChain identifier.
 */
public class IdentifierFixture {
  public static model.lightchain.Identifier newIdentifier() {
    byte[] bytes = Bytes.byteArrayFixture(model.lightchain.Identifier.Size);
    return new model.lightchain.Identifier(bytes);
  }

  /**
   * Creates an arraylist of identifiers.
   *
   * @param count total number of identifiers.
   * @return array list of created identifiers.
   */
  public static ArrayList<model.lightchain.Identifier> newIdentifiers(int count) {
    ArrayList<Identifier> identifiers = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      Identifier identifier = IdentifierFixture.newIdentifier();
      identifiers.add(identifier);
    }

    return identifiers;
  }
}
