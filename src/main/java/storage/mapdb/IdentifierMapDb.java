package storage.mapdb;

import java.util.ArrayList;

import org.mapdb.DB;
import org.mapdb.Serializer;
import org.mapdb.DBMaker;
import model.lightchain.Identifier;
import storage.Identifiers;
import org.mapdb.*;

/**
 * Implementation of identifiers interface.
 */
public class IdentifierMapDb implements Identifiers {
  private static DB db = null;
  private String filePath;
  private IndexTreeList<byte[]> identifierArrayList;

  public IdentifierMapDb(String filePath) {
    this.filePath = filePath;
    db = DBMaker.fileDB(filePath).make();
    this.identifierArrayList = db.indexTreeList("identrifierArrayList", Serializer.BYTE_ARRAY).createOrOpen();
  }

  /**
   * Adds an identifier to the storage, returns true if it is new, false if it already exists.
   *
   * @param identifier identifier to be added to storage.
   * @return true if it is new, false if it already exists.
   */
  @Override
  public boolean add(Identifier identifier) {
    byte[] bytes = identifier.getBytes();
    identifierArrayList.add(bytes);
    return identifierArrayList.add(bytes);
  }

  /**
   * Checks existence of an identifier on the storage.
   *
   * @param identifier identifier to be checked.
   * @return true if identifier exists on the storage, false otherwise.
   */
  @Override
  public boolean has(Identifier identifier) {
    byte[] bytes = identifier.getBytes();
    return identifierArrayList.contains(bytes);

  }

  /**
   * Removes an identifier from the storage.
   *
   * @param identifier identifier to be removed.
   * @return true if identifier exists and removed, false otherwise.
   */
  @Override
  public boolean remove(Identifier identifier) {
    byte[] bytes = identifier.getBytes();
    return identifierArrayList.remove(bytes);
  }

  /**
   * Returns all stored identifiers on storage.
   *
   * @return all stored identifiers on storage.
   */
  @Override
  public ArrayList<Identifier> all() {
    ArrayList<Identifier> arrayList = new ArrayList<>();
    for (byte[] element : identifierArrayList) {
      Identifier identifierFromBytes = new Identifier(element);
      arrayList.add(identifierFromBytes);
    }
    return arrayList;
  }
}
