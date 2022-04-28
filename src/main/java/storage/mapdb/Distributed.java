package storage.mapdb;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import model.Entity;
import model.codec.EncodedEntity;
import model.lightchain.Identifier;
import modules.codec.JsonEncoder;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

/**
 * Distributed databese that store encoded entities.
 */
public class Distributed implements storage.Distributed {

  private final DB db;
  private final ReentrantReadWriteLock lock;
  private static final String MAP_NAME = "distributed_map";
  private final HTreeMap distributedMap;

  /**
   * Creates DistributedMapDb.
   */
  public Distributed(String filePath) {
    this.db = DBMaker.fileDB(filePath).make();
    this.lock = new ReentrantReadWriteLock();
    distributedMap = this.db.hashMap(MAP_NAME)
        .keySerializer(Serializer.BYTE_ARRAY)
        .createOrOpen();
  }

  /**
   * Checks existence of entity on the database.
   *
   * @param entityId Identifier of entity.
   * @return true if a entity with that identifier exists, false otherwise.
   */
  @Override
  public boolean has(Identifier entityId) {
    boolean hasBoolean;
    try {
      lock.readLock().lock();
      hasBoolean = distributedMap.containsKey(entityId.getBytes());
    } finally {
      lock.readLock().unlock();
    }
    return hasBoolean;
  }

  /**
   * Adds entity to the database.
   *
   * @param e given entity to be added.
   * @return true if entity did not exist on the database, false if entity is already in
   * database.
   */
  @Override
  public boolean add(Entity e) {
    JsonEncoder encoder = new JsonEncoder();
    boolean addBoolean;
    try {
      lock.writeLock().lock();
      addBoolean = distributedMap.putIfAbsentBoolean(e.id().getBytes(), encoder.encode(e));
    } finally {
      lock.writeLock().unlock();
    }
    return addBoolean;
  }

  /**
   * Removes entity with given identifier.
   *
   * @param e identifier of the entity.
   * @return true if entity exists on database and removed successfully, false if entity does not exist on
   * database.
   */
  @Override
  public boolean remove(Entity e) {
    JsonEncoder encoder = new JsonEncoder();
    boolean removeBoolean;
    try {
      lock.writeLock().lock();
      removeBoolean = distributedMap.remove(e.id().getBytes(), encoder.encode(e));
    } finally {
      lock.writeLock().unlock();
    }
    return removeBoolean;
  }

  /**
   * Returns the entity with given identifier.
   *
   * @param entityId identifier of the entity.
   * @return the entity itself if exists and null otherwise.
   */
  @Override
  public Entity get(Identifier entityId) {

    Entity decodedEntity = null;

    try {
      JsonEncoder encoder = new JsonEncoder();
      lock.readLock().lock();
      EncodedEntity encodedEntity = (EncodedEntity) distributedMap.get(entityId.getBytes());
      if (encodedEntity == null) {
        return null;
      }
      decodedEntity = encoder.decode(encodedEntity);
    } catch (ClassNotFoundException e) {
      //throw new ClassNotFoundException("could not found the class"+e);
    } finally {
      lock.readLock().unlock();
    }
    return decodedEntity;
  }

  /**
   * Returns all entities stored in database.
   *
   * @return all stored entities in database.
   */
  @Override
  public ArrayList<Entity> all() {
    JsonEncoder encoder = new JsonEncoder();
    ArrayList<Entity> allEntities = new ArrayList<>();
    for (Object encodedEntity : distributedMap.values()) {
      try {
        allEntities.add(encoder.decode((EncodedEntity) encodedEntity));
      } catch (ClassNotFoundException e) {
        //throw new ClassNotFoundException("could not found the class"+e);
      }
    }
    return allEntities;
  }

  /**
   * It closes the database.
   */
  public void closeDb() {
    db.close();
  }
}
