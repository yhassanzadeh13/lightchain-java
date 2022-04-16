package networking.p2p;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import model.Entity;
import model.exceptions.LightChainNetworkingException;
import model.lightchain.Identifier;
import network.Conduit;
import network.Network;
import network.p2p.P2pNetwork;
import networking.MockEngine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unittest.fixtures.EntityFixture;

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
  private ArrayList<Network> networkArrayList;

  /**
   * Engine A1 (on one network) can send message to Engine A2
   * (on another network), and the message is received by Engine A2.
   */
  @Test
  void testTwoP2pNetworksTwoEngines() {
    AtomicInteger threadErrorCount = new AtomicInteger();
    P2pNetwork network1 = new P2pNetwork(PORT_ZERO);
    MockEngine engineA1 = new MockEngine();
    Conduit conduitC1 = network1.register(engineA1, channel1);

    P2pNetwork network2 = new P2pNetwork(PORT_ZERO);
    MockEngine engineA2 = new MockEngine();
    network2.register(engineA2, channel1);

    startNetworks(new P2pNetwork[]{network1, network2});

    Entity entity = new EntityFixture();

    try {
      Assertions.assertEquals(threadErrorCount.get(), 0);
      conduitC1.unicast(entity, new Identifier(("localhost:" + network2.getPort())
          .getBytes(StandardCharsets.UTF_8)));
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

    CountDownLatch unicatDone = new CountDownLatch(concurrencyDegree);

    P2pNetwork network1 = new P2pNetwork(PORT_ZERO);
    MockEngine engineA1 = new MockEngine();
    Conduit conduit1 = network1.register(engineA1, channel1);

    P2pNetwork network2 = new P2pNetwork(PORT_ZERO);
    MockEngine engineA2 = new MockEngine();
    network2.register(engineA2, channel1);

    startNetworks(new P2pNetwork[]{network1, network2});

    Thread[] unicastThreads = new Thread[concurrencyDegree];
    for (int i = 0; i < concurrencyDegree; i++) {
      unicastThreads[i] = new Thread(() -> {
        Entity entity = new EntityFixture();
        try {
          conduit1.unicast(entity, new Identifier(("localhost:" + network2.getPort())
              .getBytes(StandardCharsets.UTF_8)));
          if (!engineA2.hasReceived(entity)) {
            threadError.getAndIncrement();
          }
          unicatDone.countDown();
        } catch (LightChainNetworkingException e) {
          threadError.getAndIncrement();
        }
      });
    }

    for (Thread t : unicastThreads) {
      t.start();
    }

    try {
      boolean doneOneTime = unicatDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    Assertions.assertEquals(0, threadError.get());
  }
  //
  //  /**
  //   * Test for two P2P networks with reply.
  //   */
  //  @Test
  //  void testTwoP2pNetworksTwoEnginesReplyConcurrentMessages() {
  //    int concurrencyDegree = 100;
  //    AtomicInteger threadError = new AtomicInteger();
  //
  //    CountDownLatch countDownLatch = new CountDownLatch(concurrencyDegree);
  //    CountDownLatch countDownLatchServers = new CountDownLatch(2);
  //
  //    Thread[] unicastThreads = new Thread[concurrencyDegree];
  //
  //    P2pNetwork network1 = new P2pNetwork(PORT_ZERO);
  //    MockEngine a1 = new MockEngine();
  //    Conduit c1 = network1.register(a1, channel1);
  //
  //    P2pNetwork network2 = new P2pNetwork(PORT_ZERO);
  //    MockEngine a2 = new MockEngine();
  //    Conduit c2 = network2.register(a2, channel1);
  //
  //    Thread n1Thread = new Thread(() -> {
  //      network1.start();
  //      countDownLatchServers.countDown();
  //    });
  //
  //    Thread n2Thread = new Thread(() -> {
  //      network2.start();
  //      countDownLatchServers.countDown();
  //    });
  //
  //    n1Thread.start();
  //    n2Thread.start();
  //
  //    for (int i = 0; i < concurrencyDegree; i++) {
  //      unicastThreads[i] = new Thread(() -> {
  //        Entity entity = new EntityFixture();
  //        Entity entity2 = new EntityFixture();
  //        try {
  //          c1.unicast(entity, new Identifier(("localhost:" + network2.getPort())
  //              .getBytes(StandardCharsets.UTF_8)));
  //          if (!a2.hasReceived(entity)) {
  //            threadError.getAndIncrement();
  //          }
  //          c2.unicast(entity2, new Identifier(("localhost:" + network1.getPort())
  //              .getBytes(StandardCharsets.UTF_8)));
  //          if (!a1.hasReceived(entity2)) {
  //            threadError.getAndIncrement();
  //          }
  //          countDownLatch.countDown();
  //        } catch (LightChainNetworkingException e) {
  //          threadError.getAndIncrement();
  //        }
  //      });
  //    }
  //    for (Thread t : unicastThreads) {
  //      t.start();
  //    }
  //    try {
  //      boolean doneOneTime = countDownLatch.await(60, TimeUnit.SECONDS);
  //      Assertions.assertTrue(doneOneTime);
  //    } catch (InterruptedException e) {
  //      Assertions.fail();
  //    }
  //    Assertions.assertEquals(0, threadError.get());
  //  }
  //
  //  /**
  //   * Test two P2P networks with four engines, concurrently messages.
  //   */
  //  @Test
  //  void testTwoP2pNetworksFourEnginesConcurrentMessages() {
  //    int concurrencyDegree = 100;
  //    AtomicInteger threadError = new AtomicInteger();
  //
  //    CountDownLatch countDownLatch = new CountDownLatch(concurrencyDegree);
  //    CountDownLatch countDownLatchServers = new CountDownLatch(2);
  //
  //    Thread[] unicastThreads = new Thread[concurrencyDegree];
  //
  //    P2pNetwork network1 = new P2pNetwork(PORT_ZERO);
  //    MockEngine a = new MockEngine();
  //    Conduit c1 = network1.register(a, channel1);
  //
  //    MockEngine b = new MockEngine();
  //    Conduit c2 = network1.register(b, channel2);
  //
  //    P2pNetwork network2 = new P2pNetwork(PORT_ZERO);
  //    MockEngine c = new MockEngine();
  //    MockEngine d = new MockEngine();
  //    network2.register(c, channel1);
  //    network2.register(d, channel2);
  //
  //    Thread n1Thread = new Thread(() -> {
  //      network1.start();
  //      countDownLatchServers.countDown();
  //    });
  //
  //    Thread n2Thread = new Thread(() -> {
  //      network2.start();
  //      countDownLatchServers.countDown();
  //    });
  //
  //    n1Thread.start();
  //    n2Thread.start();
  //
  //    for (int i = 0; i < concurrencyDegree; i++) {
  //      unicastThreads[i] = new Thread(() -> {
  //        Entity entity1 = new EntityFixture();
  //        Entity entity2 = new EntityFixture();
  //        try {
  //          c1.unicast(entity1, new Identifier(("localhost:" + network2.getPort())
  //              .getBytes(StandardCharsets.UTF_8)));
  //          c2.unicast(entity2, new Identifier(("localhost:" + network2.getPort())
  //              .getBytes(StandardCharsets.UTF_8)));
  //     if (!c.hasReceived(entity1) || c.hasReceived(entity2) || !d.hasReceived(entity2) || d.hasReceived(entity1)) {
  //            threadError.getAndIncrement();
  //          }
  //          countDownLatch.countDown();
  //        } catch (LightChainNetworkingException e) {
  //          threadError.getAndIncrement();
  //        }
  //      });
  //    }
  //
  //    for (Thread t : unicastThreads) {
  //      t.start();
  //    }
  //
  //    try {
  //      boolean doneOneTime = countDownLatch.await(60, TimeUnit.SECONDS);
  //      Assertions.assertTrue(doneOneTime);
  //    } catch (InterruptedException e) {
  //      Assertions.fail();
  //    }
  //
  //    Assertions.assertEquals(0, threadError.get());
  //
  //  }
  //
  //  /**
  //   * Test for Registration to Occupied Channel.
  //   */
  //  @Test
  //  void testRegisterToOccupiedChannel() {
  //    P2pNetwork network1 = new P2pNetwork(PORT_ZERO);
  //    MockEngine a1 = new MockEngine();
  //    network1.register(a1, channel1);
  //    MockEngine b1 = new MockEngine();
  //    try {
  //      network1.register(b1, channel1);
  //      Assertions.fail("failed! method was expected to throw an exception");
  //    } catch (IllegalStateException e) {
  //      //throw new IllegalStateException("could not register to channel since its already occupied");
  //    }
  //  }

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

  }
}
