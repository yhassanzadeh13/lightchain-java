package bootstrap;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import model.Entity;
import model.exceptions.LightChainNetworkingException;
import model.lightchain.Identifier;
import modules.ComponentManager;
import modules.logger.LightchainLogger;
import modules.logger.Logger;
import network.Conduit;
import network.Network;
import network.p2p.P2pNetwork;
import protocol.Engine;

/**
 * Represents a mock implementation of Engine interface for testing.
 */
public class BroadcastEngine implements Engine {
  private static final String channel = "broadcast-channel";
  private final Logger logger;
  private final ReentrantReadWriteLock lock;
  private final Set<Identifier> receivedEntityIds;
  private final Conduit conduit;
  private final P2pNetwork network;
  ConcurrentMap<Identifier, String> idTable;
  Identifier myId;

  /**
   * Constructor for BroadcastEngine.
   *
   * @param idTable idTable containing all Nodes and their addresses.
   * @param myId    id of the Node on which the Engine operates.
   */
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "we want idTable to be exposed externally")
  public BroadcastEngine(ConcurrentMap<Identifier, String> idTable, Identifier myId, Network network) {
    this.receivedEntityIds = new HashSet<>();
    this.lock = new ReentrantReadWriteLock();
    this.logger = LightchainLogger.getLogger(BroadcastEngine.class.getCanonicalName(), myId);
    this.idTable = new ConcurrentHashMap<>();
    this.myId = myId;
    this.idTable.putAll(idTable);
    this.network = (P2pNetwork) network;
    this.conduit = network.register(this, channel);
  }

  /**
   * Called by Network whenever an Entity is arrived for this engine.
   *
   * @param e the arrived Entity from the network.
   */
  @Override
  public void process(Entity e) {
    lock.writeLock().lock();
    try {
      receivedEntityIds.add(e.id());
    } finally {
      lock.writeLock().unlock();
    }

    HelloMessageEntity helloMessageEntity = (HelloMessageEntity) e;
    logger.info("received hello message from {} with message {} (total so far {})", helloMessageEntity.getSenderId(), helloMessageEntity.getContent(),
        totalReceived());
  }

  /**
   * Total distinct entities this engine received.
   *
   * @return total messages it received.
   */
  public int totalReceived() {
    lock.readLock().lock();
    int size;

    try {
      size = receivedEntityIds.size();
    } finally {
      lock.readLock().unlock();
    }
    return size;
  }

  /**
   * Sends a hello message to all nodes in the network.
   */
  private void sendHelloMessagesToAll(int count) {
    for (int i = 0; i < count; i++) {
      try {
        TimeUnit.MILLISECONDS.sleep(1000);
      } catch (InterruptedException e) {
        this.logger.fatal("could not sleep", e);
      }

      for (Map.Entry<Identifier, String> id : idTable.entrySet()) {
        if (!id.getKey().toString().equals(myId.toString())) {
          HelloMessageEntity e = new HelloMessageEntity("# + " + i + 1 + " hello from " + myId + " to " + id.getKey(), myId);
          try {
            this.conduit.unicast(e, id.getKey());
          } catch (LightChainNetworkingException ex) {
            this.logger.fatal("could not send hello message", ex);
          }
        }
      }
    }
  }

  @Override
  public void start(Duration deadline) throws IllegalStateException {
    ComponentManager componentManager = new ComponentManager();
    componentManager.addComponent(this.network);
    componentManager.start(deadline);
    new Thread(() -> sendHelloMessagesToAll(100)).start();
  }
}
package bootstrap;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.prometheus.client.Counter;
import metrics.collectors.LightChainCollector;
import metrics.collectors.MetricServer;
import model.Entity;
import model.lightchain.Identifier;
import protocol.Engine;

/**
 * Represents a mock implementation of Engine interface for testing.
 */
public class BroadcastEngine implements Engine {
  private final ReentrantReadWriteLock lock;
  private final Set<Identifier> receivedEntityIds;

  LightChainCollector collector;
  Counter helloMessageReceiveCount;
  ConcurrentMap<Identifier, String> idTable;
  Identifier myId;
  MetricServer server;

  /**
   * Constructor for BroadcastEngine.
   */
  public BroadcastEngine() {
    this.receivedEntityIds = new HashSet<>();
    this.lock = new ReentrantReadWriteLock();
    this.server = new MetricServer(8082);
  }

  /**
   * Constructor for BroadcastEngine.
   *
   * @param idTable idTable containing all Nodes and their addresses.
   * @param myId    id of the Node on which the Engine operates.
   */
  public BroadcastEngine(ConcurrentMap<Identifier, String> idTable, Identifier myId) {
    this();
    this.idTable = new ConcurrentHashMap<>();
    this.myId = myId;
    this.idTable.putAll(idTable);

    // Metric Server Initiation
    try {
      collector = new LightChainCollector();
      // possibly change the namespace and subsystem values
      helloMessageReceiveCount = collector.counter().register("hello_message_receive_count",
              "consensus", myId.toString(), "Number of hello messages received");

    } catch (IllegalArgumentException ex) {
      throw new IllegalStateException("could not initialize the metrics with the provided arguments", ex);
    }

    try {
      server.start();
    } catch (IllegalStateException ex) {
      throw new IllegalStateException("could not start the Metric Server", ex);
    }
  }

  /**
   * Called by Network whenever an Entity is arrived for this engine.
   *
   * @param e the arrived Entity from the network.
   */
  @Override
  public void process(Entity e) {
    lock.writeLock().lock();
    try {
      receivedEntityIds.add(e.id());
      helloMessageReceiveCount.inc(1);
      System.out.println("Node " + idTable.get(myId) + ": content of the last message is: ");
      System.out.println(((HelloMessageEntity) e).content);
      System.out.println("Total Unique Entries Received " + totalReceived());
      System.out.println();
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Check whether an entity is received.
   *
   * @param e the entity.
   * @return true if the entity received, otherwise false.
   */
  public boolean hasReceived(Entity e) {
    lock.readLock().lock();
    boolean ok;

    try {
      ok = this.receivedEntityIds.contains(e.id());
    } finally {
      lock.readLock().unlock();
    }
    return ok;
  }

  /**
   * Total distinct entities this engine received.
   *
   * @return total messages it received.
   */
  public int totalReceived() {
    lock.readLock().lock();
    int size;

    try {
      size = receivedEntityIds.size();
    } finally {
      lock.readLock().unlock();
    }
    return size;
  }
}