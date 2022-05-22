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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import unittest.fixtures.EntityFixture;

/**
 * Encapsulates tests for the storage side of the stub network.
 */
public class StubNetworkStorageTest {
  private final String channel1 = "test-network-channel-1";
  private final String channel2 = "test-network-channel-2";
  private Hub hub;
  private StubNetwork stubNetwork1;
  private StubNetwork stubNetwork2;
  private MockEngine a1;
  private MockEngine b1;
  private MockEngine b2;
  private Conduit ca1;
  private Conduit cb1;
  private Conduit cb2;
  private ArrayList<Entity> allEntities;
  private ArrayList<Entity> firstChannelEntities;
  private ArrayList<Entity> secondChannelEntities;

  @BeforeEach
  void setUp() {
    this.hub = new Hub();
    stubNetwork1 = new StubNetwork(hub);
    a1 = new MockEngine();
    ca1 = stubNetwork1.register(a1, channel1);
    stubNetwork2 = new StubNetwork(hub);
    b1 = new MockEngine();
    cb1 = stubNetwork2.register(b1, channel1);
    b2 = new MockEngine();
    cb2 = stubNetwork2.register(b2, channel2);
  }

  /**
   * Engine A1 (on one network) puts an entity on channel1 and Engine B1 on another network can get it on the
   * same channel1 successfully, while Engine B2 on another channel2 can't get it successfully.
   */
  @Test
  void testPutOneEntity() {

    Entity entity = new EntityFixture();
    try {
      ca1.put(entity);
      if (!cb1.get(entity.id()).equals(entity) || cb2.get(entity.id()) != null) {
        Assertions.fail();
      }
    } catch (LightChainDistributedStorageException e) {
      Assertions.fail();
    }
  }

  /**
   * Engine A1 (on one network)  can CONCURRENTLY put 100 different entities on channel1, and
   * Engine B1 on another network can get each entity using its entity id only the the same channel,
   * while Engine B2 on another channel2 can't get it any of them successfully.
   */
  @Test
  void putEntityConcurrently() {
    this.putEntityConcurrentlyFunction();
    this.getEntityConcurrentlyFunction();
  }

  /**
   * Engine A1 (on one network)  can CONCURRENTLY put 100 different entities on channel1, and
   * Engine B1 on another network can get all of them at once using allEntities method,
   * while Engine B2 on another channel2 can't get none of them using all.
   */
  @Test
  void testAllMethodConcurrently() {
    this.putEntityConcurrentlyFunction();
    this.checkAllConcurrently();
  }

  /**
   * Engine B1 on another network can get all of them at once using allEntities method,
   * while Engine B2 on another channel2 can't get none of them using all.
   */
  private void checkAllConcurrently() {
    int concurrencyDegree = 2;
    AtomicInteger threadError = new AtomicInteger();

    CountDownLatch allDone = new CountDownLatch(concurrencyDegree);
    Thread[] entityThreads = new Thread[concurrencyDegree];
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      entityThreads[i] = new Thread(() -> {
        try {
          if (finalI == 0) {
            firstChannelEntities = cb1.allEntities();
          } else {
            secondChannelEntities = cb2.allEntities();
          }
          allDone.countDown();
        } catch (LightChainDistributedStorageException e) {
          threadError.getAndIncrement();
        }
      });
    }
    for (Thread t : entityThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = allDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    Assertions.assertEquals(0, threadError.get());
    Assertions.assertTrue(firstChannelEntities.containsAll(allEntities));
    Assertions.assertTrue(secondChannelEntities.isEmpty());
  }

  /**
   * Engine A1 (on one network)  can CONCURRENTLY put 100 different entities on channel1
   */
  private void putEntityConcurrentlyFunction() {
    int concurrencyDegree = 100;
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch putDone = new CountDownLatch(concurrencyDegree);
    Thread[] entityThreads = new Thread[concurrencyDegree];
    allEntities = new ArrayList<>();
    for (int i = 0; i < concurrencyDegree; i++) {
      entityThreads[i] = new Thread(() -> {
        Entity entity = new EntityFixture();
        allEntities.add(entity);
        try {
          ca1.put(entity);
          putDone.countDown();
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
   * Engine B1 on another network can get it on the
   * same channel1 successfully, while Engine B2 on another channel2 can't get it successfully.
   */
  private void getEntityConcurrentlyFunction() {
    int concurrencyDegree = 100;
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch getDone = new CountDownLatch(concurrencyDegree);
    Thread[] entityThreads = new Thread[concurrencyDegree];
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      entityThreads[i] = new Thread(() -> {
        Entity entity = allEntities.get(finalI);
        try {
          if (!cb1.get(entity.id()).equals(entity) || cb2.get(entity.id()) != null) {
            threadError.getAndIncrement();
          }
          getDone.countDown();
        } catch (LightChainDistributedStorageException e) {
          threadError.getAndIncrement();
        }
      });
    }
    for (Thread t : entityThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = getDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    Assertions.assertEquals(0, threadError.get());
  }
}
