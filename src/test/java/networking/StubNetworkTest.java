package networking;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import model.Entity;
import model.exceptions.LightChainNetworkingException;
import network.Conduit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unittest.fixtures.EntityFixture;

/**
 * Encapsulates tests for the stubnetwork.
 */
public class StubNetworkTest {

  private final String channel1 = "test-network-channel-1";
  private final String channel2 = "test-network-channel-2";

  /**
   * Engine A (on one stub network) can send message to Engine B (on another stub network) through its StubNetwork,
   * and the message is received by Engine B.
   */
  @Test
  void testTwoStubNetworksTwoEngines() {
    Hub hub = new Hub();
    StubNetwork networkA = new StubNetwork(hub);
    MockEngine engineA = new MockEngine();
    Conduit conduitA = networkA.register(engineA, channel1);

    StubNetwork networkB = new StubNetwork(hub);
    MockEngine engineB = new MockEngine();
    networkB.register(engineB, channel1);

    Entity entity = new EntityFixture();
    try {
      conduitA.unicast(entity, networkB.id());
    } catch (LightChainNetworkingException e) {
      Assertions.fail();
    }
    Assertions.assertTrue(engineB.hasReceived(entity));
  }

  /**
   * Engine A can CONCURRENTLY send 100 messages to Engine B through its StubNetwork,
   * and ALL messages received by Engine B.
   */
  @Test
  void testTwoStubNetworksTwoEnginesConcurrentMessages() {
    Hub hub = new Hub();

    int concurrencyDegree = 100;
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch countDownLatch = new CountDownLatch(concurrencyDegree);
    Thread[] unicastThreads = new Thread[concurrencyDegree];

    StubNetwork networkA = new StubNetwork(hub);
    MockEngine engineA = new MockEngine();
    Conduit conduitA = networkA.register(engineA, channel1);

    StubNetwork networkB = new StubNetwork(hub);
    MockEngine engineB = new MockEngine();
    networkB.register(engineB, channel1);

    for (int i = 0; i < concurrencyDegree; i++) {
      unicastThreads[i] = new Thread(() -> {
        Entity entity = new EntityFixture();
        try {
          conduitA.unicast(entity, networkB.id());
          if (!engineB.hasReceived(entity)) {
            threadError.getAndIncrement();
          }
          countDownLatch.countDown();
        } catch (LightChainNetworkingException e) {
          threadError.getAndIncrement();
        }
      });
    }

    for (Thread t : unicastThreads) {
      t.start();
    }

    try {
      boolean doneOneTime = countDownLatch.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    Assertions.assertEquals(0, threadError.get());
    Assertions.assertEquals(concurrencyDegree, engineB.totalReceived());
  }

  /**
   * Engine A can CONCURRENTLY send 100 messages to Engine B through its StubNetwork,
   * and ALL messages received by Engine B.
   * Engine B also sending a reply message to Engine A for each received messages and all replies
   * are received by Engine A.
   */
  @Test
  void testTwoStubNetworksTwoEnginesReplyConcurrentMessages() {
    Hub hub = new Hub();

    int concurrencyDegree = 100;
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch sendDone = new CountDownLatch(concurrencyDegree);
    Thread[] unicastThreads = new Thread[concurrencyDegree];

    StubNetwork networkA = new StubNetwork(hub);
    MockEngine engineA = new MockEngine();
    Conduit conduitA = networkA.register(engineA, channel1);

    StubNetwork networkB = new StubNetwork(hub);
    MockEngine engineB = new MockEngine();
    Conduit conduitB = networkB.register(engineB, channel1);

    for (int i = 0; i < concurrencyDegree; i++) {
      unicastThreads[i] = new Thread(() -> {
        Entity message = new EntityFixture();
        Entity reply = new EntityFixture();
        try {
          // A -> B
          conduitA.unicast(message, networkB.id());
          if (!engineB.hasReceived(message)) {
            threadError.getAndIncrement();
          }

          // B -> A
          conduitB.unicast(reply, networkA.id());
          if (!engineA.hasReceived(reply)) {
            threadError.getAndIncrement();
          }
          sendDone.countDown();
        } catch (LightChainNetworkingException e) {
          threadError.getAndIncrement();
        }
      });
    }

    for (Thread t : unicastThreads) {
      t.start();
    }

    try {
      boolean doneOneTime = sendDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    Assertions.assertEquals(0, threadError.get());
  }

  /**
   * Engines A1 and A2 on one StubNetwork can CONCURRENTLY send 100 messages to Engines B1 and B2 on another StubNetwork
   * (A1 -> B1) and (A2 -> B2), and each Engine only
   * receives messages destinated for it (B1 receives all messages from A1) and (B2 receives all messages from A2).
   */
  @Test
  void testTwoStubNetworksFourEnginesConcurrentMessages() {
    Hub hub = new Hub();

    int concurrencyDegree = 100;
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch sendDone = new CountDownLatch(concurrencyDegree);
    Thread[] unicastThreads = new Thread[concurrencyDegree];

    // network A
    StubNetwork networkA = new StubNetwork(hub);
    MockEngine engineA1 = new MockEngine();
    Conduit conduitA1 = networkA.register(engineA1, channel1);

    MockEngine engineA2 = new MockEngine();
    Conduit conduitA2 = networkA.register(engineA2, channel2);

    // network B
    StubNetwork networkB = new StubNetwork(hub);
    MockEngine engineB1 = new MockEngine();
    MockEngine engineB2 = new MockEngine();
    networkB.register(engineB1, channel1);
    networkB.register(engineB2, channel2);

    for (int i = 0; i < concurrencyDegree; i++) {
      unicastThreads[i] = new Thread(() -> {
        Entity messageA1toB1 = new EntityFixture();
        Entity messageA2toB2 = new EntityFixture();
        try {
          // A1 -> B1
          // A2 -> B2
          conduitA1.unicast(messageA1toB1, networkB.id());
          conduitA2.unicast(messageA2toB2, networkB.id());

          if (!engineB1.hasReceived(messageA1toB1)
              || engineB1.hasReceived(messageA2toB2)
              || !engineB2.hasReceived(messageA2toB2)
              || engineB2.hasReceived(messageA1toB1)) {
            threadError.getAndIncrement();
          }
          sendDone.countDown();
        } catch (LightChainNetworkingException e) {
          threadError.getAndIncrement();
        }
      });
    }
    for (Thread t : unicastThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = sendDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    Assertions.assertEquals(0, threadError.get());
  }

  /**
   * Stub network throws an exception if an engine is registering itself on an already taken channel.
   */
  @Test
  void testRegisterToOccupiedChannel() {
    Hub hub = new Hub();

    StubNetwork network1 = new StubNetwork(hub);
    MockEngine a1 = new MockEngine();
    network1.register(a1, channel1);
    MockEngine b1 = new MockEngine();
    try {
      network1.register(b1, channel1);
      Assertions.fail("fail! method was expected to throw an exception");
    } catch (IllegalStateException ignored) {
      // ignored
    }
  }
}