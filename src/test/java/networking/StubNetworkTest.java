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
 * Encapculates tests for Stubnetwork.
 */
public class StubNetworkTest {

  private ArrayList<Network> networkArrayList;
  private final String channel1 = "test-network-channel-1";
  private final String channel2 = "test-network-channel-2";
  private Hub hub;

  /**
   * Implement  before each test.
   */
  @BeforeEach
  void setup() {
    this.networkArrayList = new ArrayList<>();
    this.hub = new Hub();
    for (int i = 0; i < 9; i++) {
      StubNetwork stubNetwork = new StubNetwork(hub);
      Engine e1 = new MockEngine();
      Engine e2 = new MockEngine();
      stubNetwork.register(e1, channel1);
      stubNetwork.register(e2, channel2);
      networkArrayList.add(stubNetwork);
    }
  }

  /**
   * Test two stub networks with two engines.
   */
  @Test
  void testTwoStubNetworksTwoEngines() {
    String channel1 = "test-network-channel-1";
    Hub hub = new Hub();
    StubNetwork network1 = new StubNetwork(hub);
    MockEngine a1 = new MockEngine();
    Conduit c1 = network1.register(a1, channel1);
    StubNetwork network2 = new StubNetwork(hub);
    MockEngine a2 = new MockEngine();
    network2.register(a2, channel1);
    Entity entity = new EntityFixture();
    try {
      c1.unicast(entity, network2.id());
    } catch (LightChainNetworkingException e) {
      Assertions.fail();
    }
    Assertions.assertTrue(a2.hasReceived(entity));
  }

  /**
   * test two stub networks with two engines concurrently.
   */
  @Test
  void testTwoStubNetworksTwoEnginesConcurrentMessages() {
    int concurrencyDegree = 100;
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch countDownLatch = new CountDownLatch(concurrencyDegree);
    Thread[] unicastThreads = new Thread[concurrencyDegree];
    StubNetwork network1 = new StubNetwork(hub);
    MockEngine a1 = new MockEngine();
    Conduit c1 = network1.register(a1, channel1);
    StubNetwork network2 = new StubNetwork(hub);
    MockEngine a2 = new MockEngine();
    network2.register(a2, channel1);
    for (int i = 0; i < concurrencyDegree; i++) {
      unicastThreads[i] = new Thread(() -> {
        Entity entity = new EntityFixture();
        try {
          c1.unicast(entity, network2.id());
          if (!a2.hasReceived(entity)) {
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
   * Test two stub netwokrs for engines, concurrently messages.
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
    int count = 0;
    for (Network network : networkArrayList) {
      try {
        c1.unicast(entity, ((StubNetwork) network).id());
        MockEngine e1 = (MockEngine) ((StubNetwork) network).getEngine(channel1);
        MockEngine e2 = (MockEngine) ((StubNetwork) network).getEngine(channel2);
        if (!e1.hasReceived(entity)) {
          count++;
        }
        if (e2.hasReceived(entity)) {
          count++;
        }
      } catch (LightChainNetworkingException e) {
        count++;
      }
    }
    Assertions.assertEquals(0, count);
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
    for (Network network : networkArrayList) {
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
    int size = networkArrayList.size();
    int count = 0;
    List<Network> first = new ArrayList<>(networkArrayList.subList(0, size / 2));
    List<Network> second = new ArrayList<>(networkArrayList.subList(size / 2, size));
    Iterator<Network> firstit = first.iterator();
    Iterator<Network> secondit = second.iterator();
    while (firstit.hasNext() && secondit.hasNext()) {
      Network networkfh = firstit.next();
      Network networksh = secondit.next();
      try {
        c1.unicast(entity, ((StubNetwork) networkfh).id());
        MockEngine e1 = (MockEngine) ((StubNetwork) networkfh).getEngine(channel1);
        MockEngine e2 = (MockEngine) ((StubNetwork) networkfh).getEngine(channel2);
        MockEngine m1 = (MockEngine) ((StubNetwork) networksh).getEngine(channel1);
        MockEngine m2 = (MockEngine) ((StubNetwork) networksh).getEngine(channel2);
        if (!e1.hasReceived(entity)) {
          count++;
        }
        if (e2.hasReceived(entity) || m1.hasReceived(entity) || m2.hasReceived(entity)) {
          count++;
        }
      } catch (LightChainNetworkingException e) {
        count++;
      }
    }
    Assertions.assertEquals(0, count);
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
    int size = networkArrayList.size();
    List<Network> first = new ArrayList<>(networkArrayList.subList(0, size / 2));
    List<Network> second = new ArrayList<>(networkArrayList.subList(size / 2, size));
    Iterator<Network> firstit = first.iterator();
    Iterator<Network> secondit = second.iterator();
    Thread[] unicastThreads = new Thread[concurrencyDegree];
    int count = 0;
    while (firstit.hasNext() && secondit.hasNext()) {
      Network networkfh = firstit.next();
      Network networksh = secondit.next();
      unicastThreads[count] = new Thread(() -> {
        try {
          c1.unicast(entity, ((StubNetwork) networkfh).id());
          MockEngine e1 = (MockEngine) ((StubNetwork) networkfh).getEngine(channel1);
          MockEngine e2 = (MockEngine) ((StubNetwork) networkfh).getEngine(channel2);
          MockEngine m1 = (MockEngine) ((StubNetwork) networksh).getEngine(channel1);
          MockEngine m2 = (MockEngine) ((StubNetwork) networksh).getEngine(channel2);
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
   * Test two engines sends other engines sequentially.
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
    int count = 0;
    for (Network network : networkArrayList) {
      try {
        c1.unicast(entity1, ((StubNetwork) network).id());
        c2.unicast(entity2, ((StubNetwork) network).id());
        MockEngine e1 = (MockEngine) ((StubNetwork) network).getEngine(channel1);
        MockEngine e2 = (MockEngine) ((StubNetwork) network).getEngine(channel2);
        if (!e1.hasReceived(entity1) || !e2.hasReceived(entity2)) {
          count++;
        }
        if (e2.hasReceived(entity1) || e1.hasReceived(entity2)) {
          count++;
        }
      } catch (LightChainNetworkingException e) {
        count++;
      }
    }
    Assertions.assertEquals(0, count);
  }
}