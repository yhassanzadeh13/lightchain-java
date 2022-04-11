package networking;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import model.Entity;
import model.exceptions.LightChainNetworkingException;
import network.Conduit;
import network.Network;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import protocol.Engine;
import unittest.fixtures.EntityFixture;

/**
 * Encapsulates tests for the stubnetwork.
 */
public class StubNetworkTest {

  private ArrayList<Network> networks;
  private final String channel1 = "test-network-channel-1";
  private final String channel2 = "test-network-channel-2";
  private Hub hub;

  /**
   * Creates a hub with 10 connected networks, each network has two mock engines on different channels.
   */
  @BeforeEach
  void setup() {
    this.networks = new ArrayList<>();
    this.hub = new Hub();
    for (int i = 0; i < 9; i++) {
      StubNetwork stubNetwork = new StubNetwork(hub);
      stubNetwork.register(new MockEngine(), channel1);
      stubNetwork.register(new MockEngine(), channel2);
      networks.add(stubNetwork);
    }
  }

  /**
   * Engine A (on one stub network) can send message to Engine B (on another stub network) through its StubNetwork,
   * and the message is received by Engine B.
   */
  @Test
  void testTwoStubNetworksTwoEngines() {
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
   * Test for two stub networks with reply.
   */
  @Test
  void testTwoStubNetworksTwoEnginesReplyConcurrentMessages() {
    int concurrencyDegree = 100;
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch countDownLatch = new CountDownLatch(concurrencyDegree);
    Thread[] unicastThreads = new Thread[concurrencyDegree];
    StubNetwork network1 = new StubNetwork(hub);
    MockEngine a1 = new MockEngine();
    Conduit c1 = network1.register(a1, channel1);
    StubNetwork network2 = new StubNetwork(hub);
    MockEngine a2 = new MockEngine();
    Conduit c2 = network2.register(a2, channel1);
    for (int i = 0; i < concurrencyDegree; i++) {
      unicastThreads[i] = new Thread(() -> {
        Entity entity = new EntityFixture();
        Entity entity2 = new EntityFixture();
        try {
          c1.unicast(entity, network2.id());
          if (!a2.hasReceived(entity)) {
            threadError.getAndIncrement();
          }
          c2.unicast(entity2, network1.id());
          if (!a1.hasReceived(entity2)) {
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
  }

  /**
   * Test two stub netwokrs with four engines, concurrently messages.
   */
  @Test
  void testTwoStubNetworksFourEnginesConcurrentMessages() {
    int concurrencyDegree = 100;
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch countDownLatch = new CountDownLatch(concurrencyDegree);
    Thread[] unicastThreads = new Thread[concurrencyDegree];
    StubNetwork network1 = new StubNetwork(hub);
    MockEngine a = new MockEngine();
    Conduit c1 = network1.register(a, channel1);
    MockEngine b = new MockEngine();
    Conduit c2 = network1.register(b, channel2);
    StubNetwork network2 = new StubNetwork(hub);
    MockEngine c = new MockEngine();
    MockEngine d = new MockEngine();
    network2.register(c, channel1);
    network2.register(d, channel2);
    for (int i = 0; i < concurrencyDegree; i++) {
      unicastThreads[i] = new Thread(() -> {
        Entity entity1 = new EntityFixture();
        Entity entity2 = new EntityFixture();
        try {
          c1.unicast(entity1, network2.id());
          c2.unicast(entity2, network2.id());
          if (!c.hasReceived(entity1) || c.hasReceived(entity2) || !d.hasReceived(entity2) || d.hasReceived(entity1)) {
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
  }

  /**
   * Test for Registeration to Occupied Channel.
   */
  @Test
  void testRegisterToOccupiedChannel() {
    StubNetwork network1 = new StubNetwork(hub);
    MockEngine a1 = new MockEngine();
    network1.register(a1, channel1);
    MockEngine b1 = new MockEngine();
    try {
      network1.register(b1, channel1);
      Assertions.fail("fail! method was expected to throw an exception");
    } catch (IllegalStateException e) {
      //throw new IllegalStateException("could not register to channel since its already occupied");
    }
  }

  /**
   * Test for Unicast one engine to all other stub networks.
   */
  @Test
  void testUnicastOneToAllSequentially() {
    StubNetwork network1 = new StubNetwork(hub);
    MockEngine a1 = new MockEngine();
    Conduit c1 = network1.register(a1, channel1);
    Entity entity = new EntityFixture();
    for (Network network : networks) {
      try {
        c1.unicast(entity, ((StubNetwork) network).id());
        MockEngine e1 = (MockEngine) ((StubNetwork) network).getEngine(channel1);
        MockEngine e2 = (MockEngine) ((StubNetwork) network).getEngine(channel2);
        Assertions.assertTrue(e1.hasReceived(entity));
        Assertions.assertFalse(e2.hasReceived(entity));
      } catch (LightChainNetworkingException e) {
        Assertions.fail();
      }
    }
  }

  /**
   * Test one engine unicasts to all others concurrently.
   */
  @Test
  void testUnicastOneToAllConcurrently() {
    int concurrencyDegree = 9;
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch countDownLatch = new CountDownLatch(concurrencyDegree);
    StubNetwork network1 = new StubNetwork(hub);
    MockEngine a1 = new MockEngine();
    Conduit c1 = network1.register(a1, channel1);
    Entity entity = new EntityFixture();
    Thread[] unicastThreads = new Thread[concurrencyDegree];
    int count = 0;
    for (Network network : networks) {
      unicastThreads[count] = new Thread(() -> {
        try {
          c1.unicast(entity, ((StubNetwork) network).id());
          MockEngine e1 = (MockEngine) ((StubNetwork) network).getEngine(channel1);
          MockEngine e2 = (MockEngine) ((StubNetwork) network).getEngine(channel2);
          if (!e1.hasReceived(entity)) {
            threadError.getAndIncrement();
          }
          if (e2.hasReceived(entity)) {
            threadError.getAndIncrement();
          }
          countDownLatch.countDown();
        } catch (LightChainNetworkingException e) {
          threadError.getAndIncrement();
        }
      });
      count++;
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
  }

  /**
   * Test one engine sends unicast to some sequentially.
   */
  @Test
  void testUnicastOneToSomeSequentially() {
    StubNetwork network1 = new StubNetwork(hub);
    MockEngine a1 = new MockEngine();
    Conduit c1 = network1.register(a1, channel1);
    Entity entity = new EntityFixture();
    int size = networks.size();
    List<Network> first = new ArrayList<>(networks.subList(0, size / 2));
    List<Network> second = new ArrayList<>(networks.subList(size / 2, size));
    Iterator<Network> firstIt = first.iterator();
    Iterator<Network> secondIt = second.iterator();
    while (firstIt.hasNext() && secondIt.hasNext()) {
      Network networkFirst = firstIt.next();
      Network networkSecond = secondIt.next();
      try {
        c1.unicast(entity, ((StubNetwork) networkFirst).id());
        MockEngine e1 = (MockEngine) ((StubNetwork) networkFirst).getEngine(channel1);
        MockEngine e2 = (MockEngine) ((StubNetwork) networkFirst).getEngine(channel2);
        MockEngine m1 = (MockEngine) ((StubNetwork) networkSecond).getEngine(channel1);
        MockEngine m2 = (MockEngine) ((StubNetwork) networkSecond).getEngine(channel2);
        Assertions.assertTrue(e1.hasReceived(entity));
        Assertions.assertFalse(e2.hasReceived(entity) || m1.hasReceived(entity) || m2.hasReceived(entity));
      } catch (LightChainNetworkingException e) {
        Assertions.fail();
      }
    }
  }

  /**
   * Test one engine send unicast to some concurrently.
   */
  @Test
  void testUnicastOneToSomeConcurrently() {
    int concurrencyDegree = 4;
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch countDownLatch = new CountDownLatch(concurrencyDegree);
    StubNetwork network1 = new StubNetwork(hub);
    MockEngine a1 = new MockEngine();
    Conduit c1 = network1.register(a1, channel1);
    Entity entity = new EntityFixture();
    int size = networks.size();
    List<Network> first = new ArrayList<>(networks.subList(0, size / 2));
    List<Network> second = new ArrayList<>(networks.subList(size / 2, size));
    Iterator<Network> firstIt = first.iterator();
    Iterator<Network> secondIt = second.iterator();
    Thread[] unicastThreads = new Thread[concurrencyDegree];
    int count = 0;
    while (firstIt.hasNext() && secondIt.hasNext()) {
      Network networkFirst = firstIt.next();
      Network networkSecond = secondIt.next();
      unicastThreads[count] = new Thread(() -> {
        try {
          c1.unicast(entity, ((StubNetwork) networkFirst).id());
          MockEngine e1 = (MockEngine) ((StubNetwork) networkFirst).getEngine(channel1);
          MockEngine e2 = (MockEngine) ((StubNetwork) networkFirst).getEngine(channel2);
          MockEngine m1 = (MockEngine) ((StubNetwork) networkSecond).getEngine(channel1);
          MockEngine m2 = (MockEngine) ((StubNetwork) networkSecond).getEngine(channel2);
          if (!e1.hasReceived(entity)) {
            threadError.getAndIncrement();
          }
          if (e2.hasReceived(entity) || m1.hasReceived(entity) || m2.hasReceived(entity)) {
            threadError.getAndIncrement();
          }
          countDownLatch.countDown();
        } catch (LightChainNetworkingException e) {
          threadError.getAndIncrement();
        }
      });
      count++;
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
  }

  /**
   * Test two engines sends different entities other engines sequentially.
   */
  @Test
  void testUnicastOneToAll_SequentiallyTwoEngines() {
    StubNetwork network1 = new StubNetwork(hub);
    MockEngine a1 = new MockEngine();
    MockEngine a2 = new MockEngine();
    Conduit c1 = network1.register(a1, channel1);
    Conduit c2 = network1.register(a2, channel2);
    Entity entity1 = new EntityFixture();
    Entity entity2 = new EntityFixture();
    for (Network network : networks) {
      try {
        c1.unicast(entity1, ((StubNetwork) network).id());
        c2.unicast(entity2, ((StubNetwork) network).id());
        MockEngine e1 = (MockEngine) ((StubNetwork) network).getEngine(channel1);
        MockEngine e2 = (MockEngine) ((StubNetwork) network).getEngine(channel2);
        Assertions.assertTrue(e1.hasReceived(entity1) && e2.hasReceived(entity2));
        Assertions.assertFalse(e2.hasReceived(entity1) || e1.hasReceived(entity2));

      } catch (LightChainNetworkingException e) {
        Assertions.fail();
      }
    }
  }
}