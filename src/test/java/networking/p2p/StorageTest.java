package networking.p2p;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import model.Entity;
import model.exceptions.LightChainDistributedStorageException;
import model.lightchain.Identifier;
import network.Conduit;
import network.p2p.P2pConduit;
import network.p2p.P2pNetwork;
import networking.MockEngine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unittest.fixtures.EntityFixture;
import unittest.fixtures.IdentifierFixture;

/**
 * Encapsulates tests for p2p storage.
 */
public class StorageTest {
  // Add a test case for each of following scenarios, for now use a hash table per network instance for storage of
  // entities.
  // 1.Run a system with 10 networks each with one engine on channel 1 and one engine on channel 2.
  // Each engine concurrently puts 100 entities on the channel that it is registered on.
  // Then, all engines registering on channel 1 should be able to get all entities that other
  // engines have put on this channel (total of 1000), while could not get any of
  // entities that have been put on channel 2. The same should be true for engines of channel 2.
  //
  // 2. Create 10 networks, all having two mock engine registering one registering on
  // channel 1 and the other on channel 2. Each engine concurrently puts 100 entities on the channel
  // that it is registered on. Then check the Distributed storage component of
  // each stub network, and all stored entities on the Distributed storage components of each node
  // should satisfy condition 2 regarding their identifier. Moreover, the union of all stored entities
  // across all storage components should be exactly the 1000 original entities that are stored.
  // Moreover, no two Distributed storage components should share the same entity.
  //
  // 3. Create 10 networks, all having two mock engine registering one registering
  // on channel 1 and the other on channel 2.
  // Each engine concurrently puts only one entity 100 times on the channel it is registered on.
  // So a total of 10 unique entities are stored.
  // Then check the union of all stored entities across all storage components
  // should be exactly the 10 unique original entities that are stored.

  private static final int PORT_ZERO = 0;
  private final String channel1 = "test-network-channel-1";
  private final String channel2 = "test-network-channel-2";

  /**
   * Creates 10 Networks each with 2 Engines on 2 different channels. Each Engine on each Network puts 100 Entities
   * on their Channels, totaling 1000 Entities on each Channel. Tests that each Engine on each Channel can get all
   * Entities on that Channel but none from the other Channel.
   */
  @Test
  void testOne() throws InterruptedException {

    int concurrencyDegree = 10;

    P2pNetwork[] networks = new P2pNetwork[10];
    Conduit[] conduits1 = new P2pConduit[100];
    Conduit[] conduits2 = new P2pConduit[100];

    Entity[] entitiesForChannel1 = new Entity[1000];
    Entity[] entitiesForChannel2 = new Entity[1000];

    for (int i = 0; i < networks.length; i++) {

      networks[i] = new P2pNetwork(IdentifierFixture.newIdentifier(), PORT_ZERO);

      MockEngine engineA1 = new MockEngine();
      conduits1[i] = networks[i].register(engineA1, channel1);

      MockEngine engineA2 = new MockEngine();
      conduits2[i] = networks[i].register(engineA2, channel2);

    }

    startNetworks(networks);

    Thread[] threads = new Thread[concurrencyDegree];
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      threads[i] = new Thread(() -> {

        for (int j = 0; j < 100; j++) {
          entitiesForChannel1[finalI * 100 + j] = new EntityFixture();
          entitiesForChannel2[finalI * 100 + j] = new EntityFixture();
        }

      });
    }

    for (Thread t : threads) {
      t.start();
      t.join();
    }

    Thread[] threads2 = new Thread[concurrencyDegree];
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      threads2[i] = new Thread(() -> {

        for (int j = 0; j < 100; j++) {
          entitiesForChannel1[finalI * 100 + j] = new EntityFixture();
          entitiesForChannel2[finalI * 100 + j] = new EntityFixture();
          try {
            conduits1[finalI].put(entitiesForChannel1[finalI * 10 + j]);
            conduits2[finalI].put(entitiesForChannel2[finalI * 10 + j]);
          } catch (LightChainDistributedStorageException e) {
            Assertions.fail();
          }
        }

      });
    }

    for (Thread t : threads2) {
      t.start();
      t.join();
    }

    Thread[] threads3 = new Thread[concurrencyDegree];
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      threads3[i] = new Thread(() -> {

        Set<Entity> set1 = new HashSet<Entity>();
        Set<Entity> set2 = new HashSet<Entity>();

        for (int j = 0; j < 1000; j++) {
          try {
            set1.add(conduits1[finalI].get(entitiesForChannel1[j].id()));
            set2.add(conduits2[finalI].get(entitiesForChannel2[j].id()));
          } catch (LightChainDistributedStorageException e) {
            Assertions.fail();
          }
        }

        // Tests that every Entity for each Channel was retrieved by every Engine on that Channel
        Assertions.assertEquals(1000, set1.size());
        Assertions.assertEquals(1000, set2.size());

      });
    }

    for (Thread t : threads3) {
      t.start();
    }

    Thread[] threads4 = new Thread[concurrencyDegree];
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      threads4[i] = new Thread(() -> {

        Set<Entity> set1 = new HashSet<Entity>();
        Set<Entity> set2 = new HashSet<Entity>();

        for (int j = 0; j < 1000; j++) {
          try {
            set1.add(conduits2[finalI].get(entitiesForChannel1[j].id()));
            set2.add(conduits1[finalI].get(entitiesForChannel2[j].id()));
          } catch (LightChainDistributedStorageException e) {
            Assertions.fail();
          }
        }

        // Tests that none of the Entities for specific Channels were accessible by Engines on other Channels
        Assertions.assertTrue(set1.contains(null));
        Assertions.assertTrue(set2.contains(null));
        Assertions.assertEquals(set1.size(), 1);
        Assertions.assertEquals(set2.size(), 1);

      });
    }

    for (Thread t : threads4) {
      t.start();
    }

  }

  /**
   * Creates 10 Networks each with 2 Engines on 2 different channels. Each Engine on each Network puts 100 Entities
   * on their Channels, totaling 1000 Entities on each Channel. Tests that the Distributed Storage Components
   * cumulatively contain all the Entities, nothing else and no duplicates.
   */
  @Test
  void testTwo() throws InterruptedException {

    int concurrencyDegree = 10;

    P2pNetwork[] networks = new P2pNetwork[10];
    Conduit[] conduits1 = new P2pConduit[100];
    Conduit[] conduits2 = new P2pConduit[100];

    Entity[] entitiesForChannel1 = new Entity[1000];
    Entity[] entitiesForChannel2 = new Entity[1000];

    for (int i = 0; i < networks.length; i++) {

      networks[i] = new P2pNetwork(IdentifierFixture.newIdentifier(), PORT_ZERO);

      MockEngine engineA1 = new MockEngine();
      conduits1[i] = networks[i].register(engineA1, channel1);

      MockEngine engineA2 = new MockEngine();
      conduits2[i] = networks[i].register(engineA2, channel2);

    }

    startNetworks(networks);

    Thread[] threads = new Thread[concurrencyDegree];
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      threads[i] = new Thread(() -> {

        for (int j = 0; j < 100; j++) {
          entitiesForChannel1[finalI * 100 + j] = new EntityFixture();
          entitiesForChannel2[finalI * 100 + j] = new EntityFixture();
        }

      });
    }

    for (Thread t : threads) {
      t.start();
      t.join();
    }

    Thread[] threads2 = new Thread[concurrencyDegree];
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      threads2[i] = new Thread(() -> {

        for (int j = 0; j < 100; j++) {
          entitiesForChannel1[finalI * 100 + j] = new EntityFixture();
          entitiesForChannel2[finalI * 100 + j] = new EntityFixture();
          try {
            conduits1[finalI].put(entitiesForChannel1[finalI * 10 + j]);
            conduits2[finalI].put(entitiesForChannel2[finalI * 10 + j]);
          } catch (LightChainDistributedStorageException e) {
            Assertions.fail();
          }
        }

      });
    }

    for (Thread t : threads2) {
      t.start();
      t.join();
    }

    Thread[] threads3 = new Thread[concurrencyDegree];
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      threads3[i] = new Thread(() -> {

        ArrayList<Entity> list1 = new ArrayList<Entity>();
        ArrayList<Entity> list2 = new ArrayList<Entity>();

        for (int j = 0; j < 1000; j++) {
          try {
            list1.add(conduits1[finalI].get(entitiesForChannel1[j].id()));
            list2.add(conduits2[finalI].get(entitiesForChannel2[j].id()));
          } catch (LightChainDistributedStorageException e) {
            Assertions.fail();
          }
        }

        // Tests that nothing but every unique Entity for each Channel was retrieved by every Engine on that Channel
        Assertions.assertEquals(1000, list1.size());
        Assertions.assertEquals(1000, list2.size());

      });
    }

    for (Thread t : threads3) {
      t.start();
    }

    Thread[] threads4 = new Thread[concurrencyDegree];
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      threads4[i] = new Thread(() -> {

        ArrayList<Entity> list1 = new ArrayList<Entity>();
        ArrayList<Entity> list2 = new ArrayList<Entity>();

        Set<Entity> set1 = new HashSet<Entity>();
        Set<Entity> set2 = new HashSet<Entity>();

        boolean duplicatesFound = false;

        for (int j = 0; j < 1000; j++) {
          try {

            if (set1.contains(conduits1[finalI].get(entitiesForChannel1[j].id()))
                    || set2.contains(conduits2[finalI].get(entitiesForChannel2[j].id()))) {
              duplicatesFound = true;
            }

            set1.add(conduits1[finalI].get(entitiesForChannel1[j].id()));
            set2.add(conduits2[finalI].get(entitiesForChannel2[j].id()));

            list1.add(conduits2[finalI].get(entitiesForChannel1[j].id()));
            list2.add(conduits1[finalI].get(entitiesForChannel2[j].id()));
          } catch (LightChainDistributedStorageException e) {
            Assertions.fail();
          }
        }

        // Tests that none of the Entities for specific Channels were accessible by Engines on other Channels
        Assertions.assertTrue(list1.contains(null));
        Assertions.assertTrue(list2.contains(null));
        Assertions.assertEquals(list1.size(), 1);
        Assertions.assertEquals(list2.size(), 1);

        // Tests no storage elements contained duplicates
        Assertions.assertTrue(!duplicatesFound);

      });
    }

    for (Thread t : threads4) {
      t.start();
    }

  }

  /**
   * Creates 10 Networks each with 2 Engines on 2 different channels. Each Engine on each Network puts 1 unique Entity
   * on their Channels, totaling 10 Entities on each Channel. Tests that each Engine on each Channel can get all
   * Entities on that Channel but with no duplicates.
   */
  @Test
  void testThree() throws InterruptedException {

    int concurrencyDegree = 10;

    P2pNetwork[] networks = new P2pNetwork[10];
    Conduit[] conduits1 = new P2pConduit[100];
    Conduit[] conduits2 = new P2pConduit[100];

    Entity[] entitiesForChannel1 = new Entity[1000];
    Entity[] entitiesForChannel2 = new Entity[1000];

    for (int i = 0; i < networks.length; i++) {

      networks[i] = new P2pNetwork(IdentifierFixture.newIdentifier(), PORT_ZERO);

      MockEngine engineA1 = new MockEngine();
      conduits1[i] = networks[i].register(engineA1, channel1);

      MockEngine engineA2 = new MockEngine();
      conduits2[i] = networks[i].register(engineA2, channel2);

    }

    startNetworks(networks);

    Thread[] threads2 = new Thread[concurrencyDegree];
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      threads2[i] = new Thread(() -> {

        Entity entityForChannel1 = new EntityFixture();
        Entity entityForChannel2 = new EntityFixture();

        for (int j = 0; j < 100; j++) {
          try {
            conduits1[finalI].put(entityForChannel1);
            conduits2[finalI].put(entityForChannel2);
          } catch (LightChainDistributedStorageException e) {
            Assertions.fail();
          }
        }

      });
    }

    for (Thread t : threads2) {
      t.start();
      t.join();
    }

    Set<Entity> set1 = new HashSet<Entity>();
    Set<Entity> set2 = new HashSet<Entity>();

    Thread[] threads3 = new Thread[concurrencyDegree];
    for (int i = 0; i < concurrencyDegree; i++) {

      try {
        set1.addAll(conduits1[i].allEntities());
        set2.addAll(conduits2[i].allEntities());
      } catch (LightChainDistributedStorageException e) {
        Assertions.fail();
      }

    }

    // Tests that when every Engine on 10 different Nodes registers a single unique Entity, the entire data across
    // all Nodes for that Channel contains 10 unique Entities in total
    Assertions.assertEquals(10, set1.size());
    Assertions.assertEquals(10, set2.size());

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
