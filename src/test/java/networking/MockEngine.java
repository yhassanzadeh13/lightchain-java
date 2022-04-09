package networking;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import model.Entity;
import model.lightchain.Identifier;
import protocol.Engine;

/**
 * Represents a mock implementation of Engine interface for testing.
 */
public class MockEngine implements Engine {
  private final ReentrantReadWriteLock lock;
  private final Set<String> receivedEntityIds;

  public MockEngine() {
    this.receivedEntityIds = new HashSet<>();
    this.lock = new ReentrantReadWriteLock();
  }

  /**
   * Called by Network whenever an Entity is arrived for this engine.
   *
   * @param e the arrived Entity from the network.
   * @throws IllegalArgumentException any unhappy path taken on processing the Entity.
   */

  @Override
  public void process(Entity e) throws IllegalArgumentException {
    lock.writeLock();
    receivedEntityIds.add(e.id().toString());
    lock.writeLock();
  }

  /**
   * Check whether an entity is received.
   *
   * @param e the entitiy.
   * @return true if the entity received, otherwise false.
   */
  public boolean hasReceived(Entity e) {
    lock.readLock();
    Identifier id = e.id();

    System.out.println("shouldve gotten: " + e.id().toString());

    boolean ok = this.receivedEntityIds.contains(e.id().toString());

    System.out.println("is " + ok);

    for (Object i :receivedEntityIds) {
      System.out.println(i);
    }

    lock.readLock();
    return ok;
  }

}