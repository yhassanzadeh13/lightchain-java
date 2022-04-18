package bootstrap;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import model.Entity;
import model.lightchain.Identifier;
import protocol.Engine;

/**
 * Represents a mock implementation of Engine interface for testing.
 */
public class BroadcastEngine implements Engine {
  private final ReentrantReadWriteLock lock;
  private final Set<Identifier> receivedEntityIds;

  ConcurrentMap<Identifier, String> idTable;
  Identifier myId;

  public BroadcastEngine() {
    this.receivedEntityIds = new HashSet<>();
    this.lock = new ReentrantReadWriteLock();
  }

  /**
   *  Constructor for BroadcastEngine.
   *
   * @param idTable idTable containing all Nodes and their addresses.
   * @param myId id of the Node on which the Engine operates.
   */
  public BroadcastEngine(ConcurrentMap<Identifier, String> idTable, Identifier myId) {
    this.receivedEntityIds = new HashSet<>();
    this.lock = new ReentrantReadWriteLock();

    this.idTable = idTable;
    this.myId = myId;
  }

  /**
   * Called by Network whenever an Entity is arrived for this engine.
   *
   * @param e the arrived Entity from the network.
   * @throws IllegalArgumentException any unhappy path taken on processing the Entity.
   */

  @Override
  public void process(Entity e) throws IllegalArgumentException {
    lock.writeLock().lock();

    receivedEntityIds.add(e.id());

    System.out.println("The content of the last message is: ");
    System.out.println(((HelloMessageEntity) e).content);
    System.out.println("Total Unique Entries Received " + totalReceived());
    System.out.println("");

    lock.writeLock().unlock();
  }

  /**
   * Check whether an entity is received.
   *
   * @param e the entity.
   * @return true if the entity received, otherwise false.
   */
  public boolean hasReceived(Entity e) {
    lock.readLock().lock();

    boolean ok = this.receivedEntityIds.contains(e.id());

    lock.readLock().unlock();
    return ok;
  }

  /**
   * Total distinct entities this engine received.
   *
   * @return total messages it received.
   */
  public int totalReceived() {
    lock.readLock().lock();

    int size = receivedEntityIds.size();

    lock.readLock().unlock();
    return size;
  }
}