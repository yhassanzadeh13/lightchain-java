package networking.stub;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import model.Entity;
import model.exceptions.LightChainDistributedStorageException;
import network.Conduit;
import networking.MockEngine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unittest.fixtures.EntityFixture;

/**
 * Encapsulates tests for the storage side of the stub network.
 */
public class StubNetworkStorageTest {
  private final String channel1 = "test-network-channel-1";
  private final String channel2 = "test-network-channel-2";
  private Hub hub;
  // TODO: implement test scenarios
  // Use mock engines with stub network.
  // 1. Engine A1 (on one network) puts an entity on channel1 and Engine B1 on another network can get it on the
  //    same channel1 successfully, while Engine B2 on another channel2 can't get it successfully.
  // 2. Engine A1 (on one network)  can CONCURRENTLY put 100 different entities on channel1, and
  //    Engine B1 on another network can get each entity using its entity id only the the same channel,
  //    while Engine B2 on another channel2 can't get it any of them successfully.
  // 3. Engine A1 (on one network)  can CONCURRENTLY put 100 different entities on channel1, and
  //    Engine B1 on another network can get all of them at once using allEntities method,
  //    while Engine B2 on another channel2 can't get none of them using all.

  /**
   * Put an entity on channel1 from A1 and Engine B1 on another network can get it.
   */
  @Test
  void testPutOneEntity() {
    this.hub = new Hub();
    StubNetwork stubNetwork1 = new StubNetwork(hub);
    MockEngine a1 = new MockEngine();
    Conduit cA1 = stubNetwork1.register(a1, channel1);
  /*
  Create the other network.
   */
    StubNetwork stubNetwork2 = new StubNetwork(hub);
    MockEngine b1 = new MockEngine();
    Conduit cB1 = stubNetwork2.register(b1, channel1);
    MockEngine b2 = new MockEngine();
    Conduit cB2 = stubNetwork2.register(b2, channel2);
    Entity entity = new EntityFixture();
    try {
      cA1.put(entity);
      if (!cB1.get(entity.id()).equals(entity) || cB2.get(entity.id()) != null) {
        Assertions.fail();
      }
    } catch (LightChainDistributedStorageException e) {
      Assertions.fail();
    }
  }

  /**
   * Put 100 entities on channel1 concurrently and test whether correct engine on the other channel can get it.
   */
  @Test
  void putEntityConcurrently() {
    this.hub = new Hub();

    int concurrencyDegree = 100;
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch putDone = new CountDownLatch(concurrencyDegree);
    Thread[] entityThreads = new Thread[concurrencyDegree];

    StubNetwork stubNetwork1 = new StubNetwork(hub);
    MockEngine a1 = new MockEngine();
    Conduit cA1 = stubNetwork1.register(a1, channel1);

    StubNetwork stubNetwork2 = new StubNetwork(hub);
    MockEngine b1 = new MockEngine();
    Conduit cB1 = stubNetwork2.register(b1, channel1);
    MockEngine b2 = new MockEngine();
    Conduit cB2 = stubNetwork2.register(b2, channel2);
    for (int i = 0; i < concurrencyDegree; i++) {
      entityThreads[i] = new Thread(() -> {
        Entity entity = new EntityFixture();
        try {
          cA1.put(entity);
          putDone.countDown();
          if (!cB1.get(entity.id()).equals(entity) || cB2.get(entity.id()) != null) {
            threadError.getAndIncrement();
          }
        } catch (LightChainDistributedStorageException e) {
          threadError.getAndIncrement();
        }
      });
    }
    for (Thread t : entityThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = putDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    Assertions.assertEquals(0, threadError.get());
  }

  /**
   * Put 100 entities on channel1 concurrently and test whether correct engine on the other channel can get with All.
   */
  @Test
  void testAllMethodConcurrently() {
    this.hub = new Hub();

    int concurrencyDegree = 100;
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch putDone = new CountDownLatch(concurrencyDegree);
    Thread[] entityThreads = new Thread[concurrencyDegree];

    StubNetwork stubNetwork1 = new StubNetwork(hub);
    MockEngine a1 = new MockEngine();
    Conduit cA1 = stubNetwork1.register(a1, channel1);

    StubNetwork stubNetwork2 = new StubNetwork(hub);
    MockEngine b1 = new MockEngine();
    Conduit cB1 = stubNetwork2.register(b1, channel1);
    MockEngine b2 = new MockEngine();
    Conduit cB2 = stubNetwork2.register(b2, channel2);
    ArrayList<Entity> allEntities = new ArrayList<>();
    for (int i = 0; i < concurrencyDegree; i++) {
      allEntities.add(new EntityFixture());
    }
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      entityThreads[i] = new Thread(() -> {
        try {
          cA1.put(allEntities.get(finalI));
          putDone.countDown();
        } catch (LightChainDistributedStorageException e) {
          threadError.getAndIncrement();
        }
      });
    }
    for (Thread t : entityThreads) {
      t.start();
    }
    // Check the allEntities method.
    try {
      boolean doneOneTime = putDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    try {
      ArrayList<Entity> firstChannelEntities = cB1.allEntities();
      ArrayList<Entity> secondChannelEntities = cB2.allEntities();
      for (Entity entity : allEntities) {
        Assertions.assertTrue(firstChannelEntities.contains(entity)
            && !secondChannelEntities.contains(entity));
      }
    } catch (LightChainDistributedStorageException e) {
      Assertions.fail();
    }
    Assertions.assertEquals(0, threadError.get());
  }
}
