package storage.mapdb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import model.lightchain.Block;
import model.lightchain.Identifier;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.mapdb.serializer.SerializerArrayTuple;
import storage.Blocks;


/**
 * Implementation of Transactions interface.
 */
public class BlocksMapDb implements Blocks {
  private final DB db;
  private final ReentrantReadWriteLock lock;
  private static final String MAP_NAME = "blocks_map";
  private final BTreeMap<Object[],Block> blocksMap;
  private final byte[] idBytes;

  public BlocksMapDb(String filePath) {
    this.db = DBMaker.fileDB(filePath).make();
    this.lock = new ReentrantReadWriteLock();
    blocksMap = (BTreeMap<Object[], Block>) this.db.treeMap(MAP_NAME)
        .keySerializer(new SerializerArrayTuple(Serializer.BYTE_ARRAY,Serializer.INTEGER))
        .createOrOpen();
    this.idBytes=null;
  }

  /**
   * Checks existence of block on the database.
   *
   * @param blockId Identifier of block.
   * @return true if a block with that identifier exists, false otherwise.
   */
  @Override
  public boolean has(Identifier blockId) {
    NavigableMap<Object[],Block> blockNavigableMap =blocksMap.prefixSubMap(new Object[]{blockId.getBytes()});
    System.out.println(blockNavigableMap.values());
    if(!blockNavigableMap.isEmpty()){
      return true;
    }
    return false;
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
    Boolean addBool;
    try {
      lock.writeLock().lock();
  /*    System.out.println("Block id BEFORE put :"+block.id().getBytes());
      System.out.println("Block previousId BEFORE put :"+block.getPreviousBlockId());
      System.out.println("Block height BEFORE put :"+block.getHeight());*/
      Object[] objects = new Object[]{block.id().getBytes(),block.getHeight()};
      addBool= blocksMap.putIfAbsentBoolean(objects,block);

     /* System.out.println(blocksMap.getValues());
      System.out.println("Block id AFTER put :"+blocksMap.get(objects).id());
      System.out.println("Block previousID AFTER put :"+blocksMap.get(objects).getPreviousBlockId());
      System.out.println("Block height AFTER put :"+blocksMap.get(objects).getHeight());
      System.out.println();*/
    } finally {
      lock.writeLock().unlock();
    }
    return !addBool;

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
    for(Object[] objects : blocksMap.keySet()){
      if(objects[0] == blockId.getBytes()){
        return blocksMap.remove(objects,blocksMap.get(objects));
      }
    }
    return false;
  }

  /**
   * Returns the block with given identifier.
   *
   * @param blockId identifier of the block.
   * @return the block itself if exists and null otherwise.
   */
  @Override
  public Block byId(Identifier blockId) {
    NavigableMap<Object[],Block> blockNavigableMap =blocksMap.prefixSubMap(new Object[]{blockId.getBytes()});
    return blockNavigableMap.firstEntry().getValue();
  }

  /**
   * Returns the block with the given height.
   *
   * @param height height of the block.
   * @return the block itself if exists and null otherwise.
   */
  @Override
  public Block atHeight(int height) {
    /*for(byte[] bytes :)
    for(Object[] objects : blocksMap.keySet()){
      if((Integer) objects[1] == height){
        return blocksMap.get(objects);
      }
    }*/

    return null;
  }

  /**
   * Returns all blocks stored in database.
   *
   * @return all stored blocks in database.
   */
  @Override
  public ArrayList<Block> all() {
    ArrayList<Block> allBlocks =new ArrayList<>();
    for(Block block : blocksMap.getValues()){
      allBlocks.add(block);
    }
    return allBlocks;
  }
  public void closeDb() {
    db.close();
  }
}
