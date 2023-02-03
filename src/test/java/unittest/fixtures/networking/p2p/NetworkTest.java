package unittest.fixtures.networking.p2p;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import model.Entity;
import model.exceptions.LightChainNetworkingException;
import model.lightchain.Identifier;
import network.Conduit;
import network.p2p.P2pNetwork;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unittest.fixtures.EntityFixture;
import unittest.fixtures.EntityFixtureList;
import unittest.fixtures.IdentifierFixture;
import unittest.fixtures.networking.MockEngine;

/**
 * Encapsulates tests for gRPC implementation of the networking layer.
 */
public class NetworkTest {
  // Use mock engines but real p2p implemented network using gRPC
  // 1. Engine A1 (on one network) can send message to Engine A2
  //    (on another network), and the message is received by Engine A2.
  // 2. Engine A1 can CONCURRENTLY send 100 messages to Engine A2, and ALL messages received by Engine A2.
  // 3. Extend case 2 with Engine A2 also sending a reply message to Engine A1 for each received messages and all
  //    replies are received by Engine A1.
  // 4. Engines A1 and B1 on one network can CONCURRENTLY send 100 messages to Engines A2 and B2 on another network
  //    (A1 -> A2) and (B1 -> B2), and each Engine only receives messages destinated for it (A2 receives all messages
  //    from A1) and (B2 receives all messages from B1). Note that A1 and A2 must be on the same channel, and B1
  //    and B2 must be on another same channel.
  // 5. The p2p network throws an exception if an engine is registering itself on an already taken channel.

  private static final int PORT_ZERO = 0;
  private final String channel1 = "test-network-channel-1";
  private final String channel2 = "test-network-channel-2";

  /**
   * Engine A1 (on one network) can send message to Engine A2
   * (on another network), and the message is received by Engine A2.
   */
  @Test
  void testTwoP2pNetworksTwoEngines() {
    AtomicInteger threadErrorCount = new AtomicInteger();
    P2pNetwork network1 = new P2pNetwork(IdentifierFixture.newIdentifier(), PORT_ZERO);
    MockEngine engineA1 = new MockEngine();
    Conduit conduitC1 = network1.register(engineA1, channel1);

    P2pNetwork network2 = new P2pNetwork(IdentifierFixture.newIdentifier(), PORT_ZERO);
    MockEngine engineA2 = new MockEngine();
    network2.register(engineA2, channel1);

    startNetworks(new P2pNetwork[]{network1, network2});

    Entity entity = new EntityFixture();

    try {
      Assertions.assertEquals(threadErrorCount.get(), 0);
      conduitC1.unicast(entity, network2.getId());
    } catch (LightChainNetworkingException e) {
      Assertions.fail();
    }
    Assertions.assertTrue(engineA2.hasReceived(entity));
  }

  /**
   * Engine A1 can CONCURRENTLY send 100 messages to Engine A2, and ALL messages received by Engine A2.
   */
  @Test
  void testTwoP2pNetworksTwoEnginesConcurrentMessages() {
    int concurrencyDegree = 100;
    AtomicInteger threadError = new AtomicInteger();

    CountDownLatch unicastDone = new CountDownLatch(concurrencyDegree);

    P2pNetwork network1 = new P2pNetwork(IdentifierFixture.newIdentifier(), PORT_ZERO);
    MockEngine engineA1 = new MockEngine();
    Conduit conduit1 = network1.register(engineA1, channel1);

    P2pNetwork network2 = new P2pNetwork(IdentifierFixture.newIdentifier(), PORT_ZERO);
    MockEngine engineA2 = new MockEngine();
    network2.register(engineA2, channel1);

    startNetworks(new P2pNetwork[]{network1, network2});
    ArrayList<Entity> entities = EntityFixtureList.newList(concurrencyDegree);

    // unicasting each message separately.
    Thread[] unicastThreads = new Thread[concurrencyDegree];
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      unicastThreads[i] = new Thread(() -> {
        try {
          conduit1.unicast(entities.get(finalI), network2.getId());

          unicastDone.countDown();
        } catch (LightChainNetworkingException e) {
          threadError.getAndIncrement();
        }
      });
    }

    for (Thread t : unicastThreads) {
      t.start();
    }

    try {
      boolean doneOneTime = unicastDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    Assertions.assertEquals(0, threadError.get());

    // asserts engine A2 has received all entities.
    for (int i = 0; i < concurrencyDegree; i++) {
      Assertions.assertTrue(engineA2.hasReceived(entities.get(i)));
    }

  }

  /**
   * Extends testTwoP2pNetworksTwoEnginesConcurrentMessages with Engine A2 also sending
   * a reply message to Engine A1 for each received messages and all
   * replies are received by Engine A1.
   */
  @Test
  void testTwoP2pNetworksTwoEnginesReplyConcurrentMessages() {
    int concurrencyDegree = 100;
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch unicastDone = new CountDownLatch(concurrencyDegree);

    P2pNetwork network1 = new P2pNetwork(IdentifierFixture.newIdentifier(), PORT_ZERO);
    MockEngine engineA1 = new MockEngine();
    Conduit conduitC1 = network1.register(engineA1, channel1);

    P2pNetwork network2 = new P2pNetwork(IdentifierFixture.newIdentifier(), PORT_ZERO);
    MockEngine engineA2 = new MockEngine();
    Conduit conduitC2 = network2.register(engineA2, channel1);

    startNetworks(new P2pNetwork[]{network1, network2});

    ArrayList<Entity> entitiesFromA1toA2 = EntityFixtureList.newList(concurrencyDegree);
    ArrayList<Entity> entitiesFromA2toA1 = EntityFixtureList.newList(concurrencyDegree);

    // unicasts
    Thread[] unicastThreads = new Thread[concurrencyDegree];
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      unicastThreads[i] = new Thread(() -> {
        try {
          // A1 -> A2
          conduitC1.unicast(entitiesFromA1toA2.get(finalI), network2.getId());

          // A2 -> A1
          conduitC2.unicast(entitiesFromA2toA1.get(finalI), network1.getId());

          unicastDone.countDown();
        } catch (LightChainNetworkingException e) {
          threadError.getAndIncrement();
        }
      });
    }

    for (Thread t : unicastThreads) {
      t.start();
    }

    try {
      boolean doneOneTime = unicastDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    Assertions.assertEquals(0, threadError.get());

    // asserts engine both A1 and A2 has received their expected messages.
    for (int i = 0; i < concurrencyDegree; i++) {
      Assertions.assertTrue(engineA1.hasReceived(entitiesFromA2toA1.get(i)));
      Assertions.assertTrue(engineA2.hasReceived(entitiesFromA1toA2.get(i)));
    }
  }

  /**
   * Engines A1 and B1 on one network can CONCURRENTLY send 100 messages to Engines A2 and B2 on another network
   * (A1 -> A2) and (B1 -> B2), and each Engine only receives messages destinated for it (A2 receives all messages
   * from A1) and (B2 receives all messages from B1). Note that A1 and A2 must be on the same channel, and B1
   * and B2 must be on another same channel.
   */
  @Test
  void testTwoP2pNetworksFourEnginesConcurrentMessages() {
    int concurrencyDegree = 100;
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch unicastDone = new CountDownLatch(concurrencyDegree);

    P2pNetwork network1 = new P2pNetwork(IdentifierFixture.newIdentifier(), PORT_ZERO);
    MockEngine engineA1 = new MockEngine();
    Conduit conduitA1 = network1.register(engineA1, channel1);
    MockEngine engineB1 = new MockEngine();
    Conduit conduitB1 = network1.register(engineB1, channel2);

    P2pNetwork network2 = new P2pNetwork(IdentifierFixture.newIdentifier(), PORT_ZERO);
    MockEngine engineA2 = new MockEngine();
    MockEngine engineB2 = new MockEngine();
    network2.register(engineA2, channel1);
    network2.register(engineB2, channel2);

    startNetworks(new P2pNetwork[]{network1, network2});

    ArrayList<Entity> entitiesOnChannel1 = EntityFixtureList.newList(concurrencyDegree);
    ArrayList<Entity> entitiesOnChannel2 = EntityFixtureList.newList(concurrencyDegree);
    Thread[] unicastThreads = new Thread[concurrencyDegree];
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      unicastThreads[i] = new Thread(() -> {

        try {
          conduitA1.unicast(entitiesOnChannel1.get(finalI), network2.getId());
          conduitB1.unicast(entitiesOnChannel2.get(finalI), network2.getId());
          unicastDone.countDown();
        } catch (LightChainNetworkingException e) {
          threadError.getAndIncrement();
        }
      });
    }

    for (Thread t : unicastThreads) {
      t.start();
    }

    try {
      boolean doneOneTime = unicastDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }

    Assertions.assertEquals(0, threadError.get());

    // asserts engine both A2 and B2 only received expected entities on their registered channels.
    for (int i = 0; i < concurrencyDegree; i++) {
      Assertions.assertTrue(engineA2.hasReceived(entitiesOnChannel1.get(i)));
      Assertions.assertFalse(engineA2.hasReceived(entitiesOnChannel2.get(i)));

      Assertions.assertFalse(engineB2.hasReceived(entitiesOnChannel1.get(i)));
      Assertions.assertTrue(engineB2.hasReceived(entitiesOnChannel2.get(i)));
    }

  }

  /**
   * The p2p network throws an exception if an engine is registering itself on an already taken channel..
   */
  @Test
  void testRegisterToOccupiedChannel() {
    P2pNetwork network1 = new P2pNetwork(IdentifierFixture.newIdentifier(), PORT_ZERO);
    MockEngine a1 = new MockEngine();
    network1.register(a1, channel1);
    MockEngine b1 = new MockEngine();
    try {
      network1.register(b1, channel1);
      Assertions.fail("failed! method was expected to throw an exception");
    } catch (IllegalStateException e) {
      //throw new IllegalStateException("could not register to channel since its already occupied");
    }
  }

  private void startNetworks(P2pNetwork[] networks) {
    Thread[] networkThreads = new Thread[networks.length];
    AtomicInteger threadErrorCount = new AtomicInteger();
    CountDownLatch done = new CountDownLatch(networks.length);

    for (int i = 0; i < networks.length; i++) {
      int finalI = i;
      networkThreads[i] = new Thread(() -> {
        networks[finalI].start();
        done.countDown();
      });
    }

    for (int i = 0; i < networks.length; i++) {
      networkThreads[i].start();
    }
    try {
      boolean doneOneTime = done.await(10, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }

    ConcurrentMap<Identifier, String> idToAddressMap = new ConcurrentHashMap<>();
    for (P2pNetwork network : networks) {
      idToAddressMap.put(network.getId(), network.getAddress());
    }

    for (P2pNetwork network : networks) {
      network.setIdToAddressMap(idToAddressMap);
    }
  }
}
