package storage.mapdb;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import model.lightchain.Block;
import model.lightchain.Identifier;
import org.mapdb.*;
import storage.Blocks;

/**
 * Implementation of Transactions interface.
 */
public class BlocksMapDb implements Blocks {
  private final DB dbID;
  private final DB dbHeight;
  private final ReentrantReadWriteLock lock;
  private static final String MAP_NAME_ID = "blocks_map_id";
  private static final String MAP_NAME_HEIGHT = "blocks_map_height";
  private final HTreeMap blocksIdMap;
  private final HTreeMap <Integer,ArrayList<Identifier>>blocksHeightMap;


  public BlocksMapDb(String filePathId,String filePathHeight) {
    this.dbID = DBMaker.fileDB(filePathId).make();
    this.lock = new ReentrantReadWriteLock();
    blocksIdMap = this.dbID.hashMap(MAP_NAME_ID)
        .keySerializer(Serializer.BYTE_ARRAY)
        .createOrOpen();
    this.dbHeight = DBMaker.fileDB(filePathHeight).make();
    blocksHeightMap = (HTreeMap<Integer, ArrayList<Identifier>>) this.dbHeight.hashMap(MAP_NAME_HEIGHT)
        .createOrOpen();

  }

  /**
   * Checks existence of block on the database.
   *
   * @param blockId Identifier of block.
   * @return true if a block with that identifier exists, false otherwise.
   */
  @Override
  public boolean has(Identifier blockId) {
    boolean hasBoolean;
    try {
      lock.readLock().lock();
      hasBoolean = blocksIdMap.containsKey(blockId.getBytes());
    } finally {
      lock.readLock().unlock();
    }
    return hasBoolean;
  }

  /**
   * Adds block to the database.
   *
   * @param block given block to be added.
   * @return true if block did not exist on the database, false if block is already in
   * database.
   */
  @Override
  public boolean add(Block block) {
    boolean addBooleanId;
    Integer integer = block.getHeight();
    try {
      lock.writeLock().lock();
      addBooleanId = blocksIdMap.putIfAbsentBoolean(block.id().getBytes(), block);
      if (addBooleanId){
        blocksHeightMap.compute(integer,(key,value)->{
          final ArrayList<Identifier> newBlockArray;
          if(value == null){
            newBlockArray = new ArrayList<>();
          } else {
            newBlockArray = new ArrayList<>(value);
          }
          newBlockArray.add(block.id());
          return  newBlockArray;
        });
      }
    } finally {
      lock.writeLock().unlock();
    }
    return addBooleanId;

  }

  /**
   * Removes block with given identifier.
   *
   * @param blockId identifier of the block.
   * @return true if block exists on database and removed successfully, false if block does not exist on
   * database.
   */
  @Override
  public boolean remove(Identifier blockId) {
    boolean removeBoolean;
    try {
      lock.writeLock().lock();
      Block block = byId(blockId);
      removeBoolean = blocksIdMap.remove(blockId.getBytes(), block);
      if(removeBoolean){
       blocksHeightMap.get(block.getHeight()).remove(blockId);
      }
    } finally {
      lock.writeLock().unlock();
    }
    return removeBoolean;
  }

  /**
   * Returns the block with given identifier.
   * t
   * @param blockId identifier of the block.
   * @return the block itself if exists and null otherwise.
   */
  @Override
  public Block byId(Identifier blockId) {
    lock.readLock().lock();
    Block block = (Block) blocksIdMap.get(blockId.getBytes());

    lock.readLock().unlock();
    return block;
  }

  /**
   * Returns the block with the given height.
   *
   * @param height height of the block.
   * @return the block itself if exists and null otherwise.
   */
  @Override
  public Block atHeight(int height) {
    lock.readLock().lock();
    Identifier identifier =  blocksHeightMap.get(height).get(0);
    Block block = byId(identifier);
    lock.readLock().unlock();
    return block;
  }

  /**
   * Returns all blocks stored in database.
   *
   * @return all stored blocks in database.
   */
  @Override
  public ArrayList<Block> all() {
    ArrayList<Block> allBlocks = new ArrayList<>();
    for (Object block : blocksIdMap.values()) {
      allBlocks.add((Block) block);
    }
    return allBlocks;
  }
  public void closeDb() {
    dbID.close();
    dbHeight.close();
  }

}
