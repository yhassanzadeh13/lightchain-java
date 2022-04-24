package storage.mapdb;

import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import model.Entity;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import storage.Entities;

public class EntityMapDb implements Entities {

  private final DB db;
  private final ReentrantReadWriteLock lock;
  private static final String MAP_NAME = "entity_map";
  private final HTreeMap<byte[], byte[]> entityMap;

  public EntityMapDb(String filePath) {
    this.db = DBMaker.fileDB(filePath).make();
    this.lock = new ReentrantReadWriteLock();
    this.entityMap = db.hashMap(MAP_NAME).
            keySerializer(Serializer.BYTE_ARRAY)
            .valueSerializer(Serializer.BYTE_ARRAY)
            .createOrOpen();
  }

  @Override
  public boolean add(Entity entity) {
    boolean addBoolean;
    try {
      lock.writeLock().lock();
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream out = new ObjectOutputStream(bos);
      out.writeObject(entity);
      out.flush();
      byte[] entityBytes = bos.toByteArray();
      addBoolean = entityMap.putIfAbsentBoolean(entityBytes, entityBytes);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      lock.writeLock().unlock();
    }
    return addBoolean;
  }

  @Override
  public boolean has(Entity entity) {
    boolean hasBoolean;
    try {
      lock.readLock().lock();
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream out = new ObjectOutputStream(bos);
      out.writeObject(entity);
      out.flush();
      byte[] entityBytes = bos.toByteArray();
      hasBoolean = entityMap.containsKey(entityBytes);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      lock.readLock().unlock();
    }
    return hasBoolean;
  }

  @Override
  public boolean remove(Entity entity) {
    byte[] entityBytes;
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream out = new ObjectOutputStream(bos);
      out.writeObject(entity);
      out.flush();
      entityBytes = bos.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return entityMap.remove(entityBytes, entityBytes);
  }

  @Override
  public ArrayList<Entity> all() {
    ArrayList<Entity> entities = new ArrayList<>();
    for (byte[] element : entityMap.keySet()) {
      try {
        ByteArrayInputStream bis = new ByteArrayInputStream(element);
        ObjectInputStream inp = null;
        inp = new ObjectInputStream(bis);
        Entity entity = (Entity) inp.readObject();
        entities.add(entity);
      } catch (IOException e) {
        e.printStackTrace();
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
    return entities;
  }

  public void closeDb() {
    db.close();
  }
}
