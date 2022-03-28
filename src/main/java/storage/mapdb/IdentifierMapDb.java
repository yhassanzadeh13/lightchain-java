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
  private final String filePath;
  private IndexTreeList<byte[]> identifierArrayList;

  public IdentifierMapDb(String filePath) {
    this.filePath = filePath;
    db = DBMaker.fileDB(filePath).make();
    identifierArrayList = db.indexTreeList("identrifierArrayList", Serializer.BYTE_ARRAY).createOrOpen();
    db.close();
  }

  /**
   * Adds an identifier to the storage, returns true if it is new, false if it already exists.
   *
   * @param identifier identifier to be added to storage.
   * @return true if it is new, false if it already exists.
   */
  @Override
  public boolean add(Identifier identifier) {
    if(db.isClosed()){
      db = DBMaker.fileDB(filePath).make();
    }
    identifierArrayList = db.indexTreeList("identrifierArrayList", Serializer.BYTE_ARRAY).createOrOpen();
    byte[] bytes = identifier.getBytes();
    if (identifierArrayList.add(bytes)) {
      db.commit();
      db.close();
      return true;
    } else {
      db.close();
    }
    return false;
  }

  /**
   * Checks existence of an identifier on the storage.
   *
   * @param identifier identifier to be checked.
   * @return true if identifier exists on the storage, false otherwise.
   */
  @Override
  public boolean has(Identifier identifier) {
    if(db.isClosed()){
      db = DBMaker.fileDB(filePath).make();
    }
    identifierArrayList = db.indexTreeList("identrifierArrayList", Serializer.BYTE_ARRAY).createOrOpen();
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
    if(db.isClosed()){
      db = DBMaker.fileDB(filePath).make();
    }
    identifierArrayList = db.indexTreeList("identrifierArrayList", Serializer.BYTE_ARRAY).createOrOpen();
    byte[] bytes = identifier.getBytes();
    if (identifierArrayList.remove(bytes)) {
      db.commit();
      db.close();
    } else {
      db.close();
    }
    return false;
  }

  /**
   * Returns all stored identifiers on storage.
   *
   * @return all stored identifiers on storage.
   */
  @Override
  public ArrayList<Identifier> all() {
    if(db.isClosed()){
      db = DBMaker.fileDB(filePath).make();
    }
    identifierArrayList = db.indexTreeList("identrifierArrayList", Serializer.BYTE_ARRAY).createOrOpen();
    ArrayList<Identifier> arrayList = new ArrayList<>();
    for (byte[] element : identifierArrayList) {
      Identifier identifierFromBytes = new Identifier(element);
      arrayList.add(identifierFromBytes);
    }
    return arrayList;
  }
}