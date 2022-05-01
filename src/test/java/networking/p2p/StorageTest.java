package networking.p2p;

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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
  void testOne() {

    P2pNetwork[] networks = new P2pNetwork[10];
    Conduit[] conduits1 = new P2pConduit[10];
    Conduit[] conduits2 = new P2pConduit[10];

    Entity[] entitiesForChannel1 = new Entity[100];
    Entity[] entitiesForChannel2 = new Entity[100];

    int entityCounter1 = 0;
    int entityCounter2 = 0;

    for (int i = 0; i < networks.length; i++) {

      networks[i] = new P2pNetwork(IdentifierFixture.newIdentifier(), PORT_ZERO);

      MockEngine engineA1 = new MockEngine();
      conduits1[i] = networks[i].register(engineA1, channel1);

      MockEngine engineA2 = new MockEngine();
      conduits2[i] = networks[i].register(engineA2, channel2);

    }

    for (int i = 0; i < networks.length; i++) {
      for (int j = 0; j < 10; j++) {
        entitiesForChannel1[entityCounter1++] = new EntityFixture();
        entitiesForChannel2[entityCounter2++] = new EntityFixture();
      }
    }

    AtomicInteger threadErrorCount = new AtomicInteger();

    startNetworks(networks);

    try {
      Assertions.assertEquals(threadErrorCount.get(), 0);

      entityCounter1 = 0;
      entityCounter2 = 0;

      for (int i = 0; i < networks.length; i++) {
        for (int j = 0; j < 10; j++) {
          conduits1[i].put(entitiesForChannel1[entityCounter1++]);
          conduits2[i].put(entitiesForChannel2[entityCounter2++]);
        }
      }

      Set<Entity> set1;
      Set<Entity> set2;

      for (int i = 0; i < networks.length; i++) {

        set1 = new HashSet<Entity>();
        set2 = new HashSet<Entity>();

        entityCounter1 = 0;
        entityCounter2 = 0;

        for (int j = 0; j < 100; j++) {
          set1.add(conduits1[i].get(entitiesForChannel1[entityCounter1++].id()));
          set2.add(conduits2[i].get(entitiesForChannel2[entityCounter2++].id()));
        }

        // Tests that every Entity for each Channel was retrieved by every Engine on that Channel
        Assertions.assertEquals(100, set1.size());
        Assertions.assertEquals(100, set2.size());

      }

      for (int i = 0; i < networks.length; i++) {

        set1 = new HashSet<Entity>();
        set2 = new HashSet<Entity>();

        entityCounter1 = 0;
        entityCounter2 = 0;

        for (int j = 0; j < 100; j++) {
          set1.add(conduits2[i].get(entitiesForChannel1[entityCounter1++].id()));
          set2.add(conduits1[i].get(entitiesForChannel2[entityCounter2++].id()));
        }

        // Tests that none of the Entities for specific Channels were accessible by Engines on other Channels
        Assertions.assertTrue(set1.contains(null));
        Assertions.assertTrue(set2.contains(null));
        Assertions.assertEquals(set1.size(), 1);
        Assertions.assertEquals(set2.size(), 1);

      }


    } catch (LightChainDistributedStorageException e) {
      Assertions.fail();
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
