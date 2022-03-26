package networking;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import model.Entity;
import model.exceptions.LightChainNetworkingException;
import model.lightchain.Identifier;
import network.Conduit;
import network.Network;
import protocol.Engine;

/**
 * Represents a mock implementation of Engine interface for testing.
 */
public class MockEngine implements Engine {
  private final ReentrantReadWriteLock lock;
  private final Set<Identifier> receivedEntityIds;


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

    receivedEntityIds.add(e.id());

    lock.writeLock();
  }

  public boolean hasReceived(Entity e) {
    lock.readLock();

    Identifier id = e.id();
    boolean ok = this.receivedEntityIds.contains(id);

    lock.readLock();
    return ok;
  }
}