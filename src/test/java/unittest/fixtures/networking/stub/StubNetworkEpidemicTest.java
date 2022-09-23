package unittest.fixtures.networking.stub;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import model.Entity;
import model.exceptions.LightChainNetworkingException;
import network.Conduit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import unittest.fixtures.EntityFixture;
import unittest.fixtures.networking.MockEngine;

/**
 * Encapsulates one-to-all test cases of stub network.
 */
public class StubNetworkEpidemicTest {
  private final String channel1 = "test-network-channel-1";
  private final String channel2 = "test-network-channel-2";
  private ArrayList<StubNetwork> networks;
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
   * Test for Unicast one engine to all other stub networks.
   */
  @Test
  void testUnicastOneToAllSequentially() {
    StubNetwork network1 = new StubNetwork(this.hub);
    MockEngine a1 = new MockEngine();
    Conduit c1 = network1.register(a1, channel1);
    Entity entity = new EntityFixture();

    for (int i = 0; i < networks.size(); i++) {
      try {
        c1.unicast(entity, networks.get(i).id());
        MockEngine e1 = (MockEngine) networks.get(i).getEngine(channel1);
        MockEngine e2 = (MockEngine) networks.get(i).getEngine(channel2);

        // only engine on channel-1 should receive the entity.
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
    CountDownLatch sendDone = new CountDownLatch(concurrencyDegree);

    StubNetwork network1 = new StubNetwork(hub);
    MockEngine a1 = new MockEngine();
    Conduit c1 = network1.register(a1, channel1);

    Entity entity = new EntityFixture();
    Thread[] unicastThreads = new Thread[concurrencyDegree];

    for (int i = 0; i < networks.size(); i++) {
      int finalI = i;
      unicastThreads[i] = new Thread(() -> {
        try {
          c1.unicast(entity, (this.networks.get(finalI).id()));
          MockEngine e1 = (MockEngine) this.networks.get(finalI).getEngine(channel1);
          MockEngine e2 = (MockEngine) this.networks.get(finalI).getEngine(channel2);
          if (!e1.hasReceived(entity)) {
            threadError.getAndIncrement();
          }
          if (e2.hasReceived(entity)) {
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
   * Test one engine sends unicast to some sequentially.
   */
  @Test
  void testUnicastOneToSomeSequentially() {
    StubNetwork network1 = new StubNetwork(hub);
    MockEngine a1 = new MockEngine();
    Conduit c1 = network1.register(a1, channel1);

    Entity entity = new EntityFixture();

    // unicast only to the first half
    for (int i = 0; i < networks.size() / 2; i++) {
      try {
        c1.unicast(entity, this.networks.get(i).id());
      } catch (LightChainNetworkingException e) {
        Assertions.fail();
      }
    }

    // checks only first half of network should receive it.
    for (int i = 0; i < networks.size(); i++) {
      // first half of networks should receive unicast
      MockEngine e1 = (MockEngine) this.networks.get(i).getEngine(channel1);
      MockEngine e2 = (MockEngine) this.networks.get(i).getEngine(channel2);
      if (i < networks.size() / 2) {

        Assertions.assertTrue(e1.hasReceived(entity) // only engine on channel-1 should receive it.
            && !e2.hasReceived(entity));
      } else {
        Assertions.assertFalse(e1.hasReceived(entity) || e2.hasReceived(entity));
      }
    }

  }

  /**
   * Test one engine send unicast to some concurrently.
   */
  @Test
  void testUnicastOneToSomeConcurrently() {
    int concurrencyDegree = networks.size() / 2;
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch sentDone = new CountDownLatch(concurrencyDegree);
    StubNetwork network1 = new StubNetwork(hub);

    MockEngine a1 = new MockEngine();
    Conduit c1 = network1.register(a1, channel1);
    Entity entity = new EntityFixture();
    Thread[] unicastThreads = new Thread[concurrencyDegree];

    // concurrently unicasts to the first half of network
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      unicastThreads[i] = new Thread(() -> {
        try {
          c1.unicast(entity, this.networks.get(finalI).id());
          sentDone.countDown();
        } catch (LightChainNetworkingException e) {
          threadError.getAndIncrement();
        }
      });
    }

    for (Thread t : unicastThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = sentDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }

    Assertions.assertEquals(0, threadError.get());

    // checks only first half of network should receive it.
    for (int i = 0; i < networks.size(); i++) {
      // first half of networks should receive unicast
      MockEngine e1 = (MockEngine) this.networks.get(i).getEngine(channel1);
      MockEngine e2 = (MockEngine) this.networks.get(i).getEngine(channel2);
      if (i < networks.size() / 2) {

        Assertions.assertTrue(e1.hasReceived(entity) // only engine on channel-1 should receive it.
            && !e2.hasReceived(entity));
      } else {
        Assertions.assertFalse(e1.hasReceived(entity) || e2.hasReceived(entity));
      }
    }

  }

  /**
   * Test two engines sends different distinct entities over distinct channels other engines sequentially.
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

    for (StubNetwork network : networks) {
      try {
        c1.unicast(entity1, network.id());
        c2.unicast(entity2, network.id());
        MockEngine e1 = (MockEngine) network.getEngine(channel1);
        MockEngine e2 = (MockEngine) network.getEngine(channel2);
        Assertions.assertTrue(e1.hasReceived(entity1) && e2.hasReceived(entity2));
        Assertions.assertFalse(e2.hasReceived(entity1) || e1.hasReceived(entity2));

      } catch (LightChainNetworkingException e) {
        Assertions.fail();
      }
    }
  }
}
