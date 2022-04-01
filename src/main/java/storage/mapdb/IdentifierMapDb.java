package storage.mapdb;

import java.util.ArrayList;

import model.lightchain.Identifier;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import storage.Identifiers;

/**
 * Implementation of identifiers interface.
 */
public class IdentifierMapDb implements Identifiers {
  private final DB db ;
  private static final String MAP_NAME = "identifierMap";
  private final HTreeMap<byte[], byte[]> identifierMap;

  /**
   * Creates MapDb.
   *
   * @param filePath the path of the file.
   */
  public IdentifierMapDb(String filePath) {
    this.db = DBMaker.fileDB(filePath).make();
    identifierMap = this.db.hashMap(MAP_NAME)
        .keySerializer(Serializer.BYTE_ARRAY)
        .valueSerializer(Serializer.BYTE_ARRAY)
        .createOrOpen();
  }

  /**
   * Adds an identifier to the storage, returns true if it is new, false if it already exists.
   *
   * @param identifier identifier to be added to storage.
   * @return true if it is new, false if it already exists.
   */
  @Override
  public boolean add(Identifier identifier) {
    return identifierMap.putIfAbsentBoolean(identifier.getBytes(), identifier.getBytes());
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
    return identifierMap.containsKey(bytes);
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
    return identifierMap.remove(bytes, bytes);
  }

  /**
   * Returns all stored identifiers on storage.
   *
   * @return all stored identifiers on storage.
   */
  @Override
  public ArrayList<Identifier> all() {
    ArrayList<Identifier> arrayList = new ArrayList<>();
    for (byte[] element : identifierMap.keySet()) {
      Identifier identifierFromBytes = new Identifier(element);
      arrayList.add(identifierFromBytes);
    }
    return arrayList;
  }

  /**
   * It closes the database.
   */
  public void closeDb() {
    db.close();
  }
}

