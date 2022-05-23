package networking.p2p;

import java.io.IOException;
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
import networking.MockEngine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unittest.fixtures.EntityFixture;
import unittest.fixtures.EntityFixtureList;
import unittest.fixtures.IdentifierFixture;

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
   *Create 10 networks, all having a mock engine registering to the same channels. The first network unicasts an entity
   * fixture concurrently to all other engines, and other engines should receive it.
   */
  @Test
  void testTenP2pNetworksOneToAll() {
    int concurrencyDegree = 9;
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch unicastDone = new CountDownLatch(concurrencyDegree);

    P2pNetwork[] p2pNetworks = new P2pNetwork[concurrencyDegree + 1];
    ArrayList<MockEngine> enginesChannel1 = new ArrayList<>();
    P2pNetwork network1 = new P2pNetwork(IdentifierFixture.newIdentifier(), PORT_ZERO);
    MockEngine engine1 = new MockEngine();
    p2pNetworks[0] = network1;
    Conduit conduit = network1.register(engine1, channel1);
    Thread[] unicastThreads = new Thread[concurrencyDegree];
    for (int i = 1; i <= concurrencyDegree; i++) {
      P2pNetwork network = new P2pNetwork(IdentifierFixture.newIdentifier(), PORT_ZERO);
      MockEngine engine = new MockEngine();
      network.register(engine, channel1);
      enginesChannel1.add(engine);
      p2pNetworks[i] = network;
    }
    startNetworks(p2pNetworks);
    Entity entity = new EntityFixture();
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      unicastThreads[i] = new Thread(() -> {
        try {
          conduit.unicast(entity, p2pNetworks[finalI + 1].getId());
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
    for (MockEngine engine : enginesChannel1) {

      Assertions.assertTrue(engine.hasReceived(entity));
    }
    Assertions.assertEquals(0, threadError.get());
  }

  /**
   * Create 10 networks, all having a mock engine registering to the same channels.
   * Each network unicasts an entity fixture concurrently to all other engines, and other engines should receive it.
   */
  @Test
  void testTenP2pNetworksAllToAll() {
    int concurrencyDegree = 100;
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch unicastDone = new CountDownLatch(concurrencyDegree);
    P2pNetwork[] p2pNetworks = new P2pNetwork[10];
    ArrayList<MockEngine> enginesChannel1 = new ArrayList<>();
    Thread[] unicastThreads = new Thread[concurrencyDegree];
    ArrayList<Conduit> conduits = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      P2pNetwork network = new P2pNetwork(IdentifierFixture.newIdentifier(), PORT_ZERO);
      MockEngine engine = new MockEngine();
      Conduit conduit = network.register(engine, channel1);
      conduits.add(conduit);
      enginesChannel1.add(engine);
      p2pNetworks[i] = network;
    }
    startNetworks(p2pNetworks);
    int counter = 0;
    for (int j = 0; j < 10; j++) {
      int finalJ = j;
      for (int i = 0; i < 10; i++) {
        int finalI = i;
        unicastThreads[counter] = new Thread(() -> {
          try {
            Entity entity = new EntityFixture();
            if (finalI != finalJ) {
              conduits.get(finalJ).unicast(entity, p2pNetworks[finalI].getId());
              if (!enginesChannel1.get(finalI).hasReceived(entity)) {
                threadError.getAndIncrement();
              }
            }
            unicastDone.countDown();
          } catch (LightChainNetworkingException e) {
            threadError.getAndIncrement();
          }
        });
        counter++;
      }
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
  }

  /**
   * Create 10 networks, all having two mock engine registering one registering on channel 1 and the other on channel
   * 2. Each network unicasts an entity fixture concurrently to all other engines, and other engines should receive it.
   * Engines registering on different channel should not receive each others’ unicasts.
   */
  @Test
  void testTenP2pNetworksWithTwoEnginesAllToAll() {
    int concurrencyDegree = 200;
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch unicastDone = new CountDownLatch(concurrencyDegree);
    P2pNetwork[] p2pNetworks = new P2pNetwork[10];
    ArrayList<MockEngine> enginesChannel1 = new ArrayList<>();
    ArrayList<MockEngine> enginesChannel2 = new ArrayList<>();
    Thread[] unicastThreads = new Thread[concurrencyDegree];
    ArrayList<Conduit> conduits1 = new ArrayList<>();
    ArrayList<Conduit> conduits2 = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      P2pNetwork network = new P2pNetwork(IdentifierFixture.newIdentifier(), PORT_ZERO);
      MockEngine engine1 = new MockEngine();
      MockEngine engine2 = new MockEngine();
      Conduit conduit1 = network.register(engine1, channel1);
      Conduit conduit2 = network.register(engine2, channel2);
      conduits1.add(conduit1);
      conduits2.add(conduit2);
      enginesChannel1.add(engine1);
      enginesChannel2.add(engine2);
      p2pNetworks[i] = network;
    }
    startNetworks(p2pNetworks);
    int counter = 0;
    for (int k = 0; k < 2; k++) {
      int finalK = k;
      for (int j = 0; j < 10; j++) {
        int finalJ = j;
        for (int i = 0; i < 10; i++) {
          int finalI = i;
          unicastThreads[counter] = new Thread(() -> {
            try {
              Entity entity1 = new EntityFixture();
              Conduit conduit;
              MockEngine engine;
              if (finalK == 0) {
                conduit = conduits1.get(finalJ);
                engine = enginesChannel1.get(finalI);
              } else {
                conduit = conduits2.get(finalJ);
                engine = enginesChannel2.get(finalI);
              }

              if (finalI != finalJ) {
                conduit.unicast(entity1, p2pNetworks[finalI].getId());

                if (!engine.hasReceived(entity1)) {
                  threadError.getAndIncrement();
                }
              }
              unicastDone.countDown();
            } catch (LightChainNetworkingException e) {
              threadError.getAndIncrement();
            }
          });
          counter++;
        }
      }
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
    /*
    We know that they received the correct ones. Check if there is redundancy.
     */
    for (MockEngine mockEngine : enginesChannel1) {
      Assertions.assertEquals(9, mockEngine.totalReceived());
    }
    for (MockEngine mockEngine : enginesChannel2) {
      Assertions.assertEquals(9, mockEngine.totalReceived());
    }
    Assertions.assertEquals(0, threadError.get());
  }

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
        try {
          networks[finalI].start();
        } catch (IOException e) {
          threadErrorCount.incrementAndGet();
        }
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
