package storage;

import java.util.ArrayList;

import model.lightchain.Block;
import model.lightchain.Identifier;


/**
 * Persistent module for storing blocks on the disk.
 */
public interface Blocks {
  // TODO: refactor blocks to keep validated blocks.
  public static final String TAG_LAST_FINALIZED_BLOCK = "tag-last-finalized-block";
  public static final String TAG_LAST_PROPOSED_BLOCK = "tag-last-proposed-block";

  /**
   * Checks existence of block on the database.
   *
   * @param blockId Identifier of block.
   * @return true if a block with that identifier exists, false otherwise.
   */
  boolean has(Identifier blockId);

  /**
   * Adds block to the database.
   *
   * @param block given block to be added.
   * @return true if block did not exist on the database, false if block is already in
   * database.
   */
  boolean add(Block block);

  /**
   * Removes block with given identifier.
   *
   * @param blockId identifier of the block.
   * @return true if block exists on database and removed successfully, false if block does not exist on
   * database.
   */
  boolean remove(Identifier blockId);

  /**
   * Returns the block with given identifier.
   *
   * @param blockId identifier of the block.
   * @return the block itself if exists and null otherwise.
   */
  Block byId(Identifier blockId);

  /**
   * Returns the block with the given height.
   *
   * @param height height of the block.
   * @return the block itself if exists and null otherwise.
   */
  Block atHeight(int height);

  /**
   * Retrieves a block by a given tag, e.g., last finalized block.
   *
   * @param tag the tag by which the block is queried.
   * @return the block corresponding to the tag (if exists), NULL otherwise.
   */
  Block byTag(String tag);

  /**
   * Updates the block associated with the tag in database, if a block exists associated with the tag, it overriden by
   * new block, otherwise, the new block is added for the tag.
   *
   * @param tag the tag by which the block is stored.
   * @param block the block corresponding to the tag.
   */
  void updateTag(String tag, Block block);

  /**
   * Returns all blocks stored in database.
   *
   * @return all stored blocks in database.
   */
  ArrayList<Block> all();
}
