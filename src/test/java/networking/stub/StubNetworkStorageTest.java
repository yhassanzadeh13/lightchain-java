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
   * With 10 networks each with one engine on channel 1 and one engine on channel 2. Each engine concurrently puts 100
   * entities on the channel that it is registered on. Then, all engines registering on channel 1 should be able to get
   * all entities that other engines have put on this channel (total of 1000), while could not get any of entities that
   * have been put on channel 2. The same should be true for engines of channel 2.
   */
  @Test
  void putAndGetConcurrently() throws LightChainDistributedStorageException {
    int concurrencyDegree = 2000;
    AtomicInteger threadError = new AtomicInteger();
    Thread[] putThreads = new Thread[concurrencyDegree];
    CountDownLatch putDone = new CountDownLatch(concurrencyDegree);
    ArrayList<Conduit> conduits1 = new ArrayList<>();
    ArrayList<Conduit> conduits2 = new ArrayList<>();
    ArrayList<Entity> allEntitiesChannel1 = new ArrayList<>();
    ArrayList<Entity> allEntitiesChannel2 = new ArrayList<>();
    Hub hub = new Hub();
    for (int i = 0; i < 10; i++) {
      StubNetwork stubNetwork = new StubNetwork(hub);
      MockEngine engine1 = new MockEngine();
      MockEngine engine2 = new MockEngine();
      Conduit conduit1 = stubNetwork.register(engine1, channel1);
      Conduit conduit2 = stubNetwork.register(engine2, channel2);
      conduits1.add(conduit1);
      conduits2.add(conduit2);
    }
    int counter = 0;
    for (int k = 0; k < 2; k++) {
      int finalK = k;
      for (int j = 0; j < 10; j++) {
        int finalJ = j;
        for (int i = 0; i < 100; i++) {
          putThreads[counter] = new Thread(() -> {
            try {
              Conduit conduit;
              if (finalK == 0) {
                Entity entity1 = new EntityFixture();
                allEntitiesChannel1.add(entity1);
                conduit = conduits1.get(finalJ);
                conduit.put(entity1);
              } else {
                Entity entity1 = new EntityFixture();
                allEntitiesChannel2.add(entity1);
                conduit = conduits2.get(finalJ);
                conduit.put(entity1);
              }

              putDone.countDown();
            } catch (LightChainDistributedStorageException e) {
              threadError.getAndIncrement();
            }
          });
          counter++;
        }
      }
    }
    for (Thread t : putThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = putDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    for (Conduit conduit : conduits1) {
      Assertions.assertTrue(conduit.allEntities().containsAll(allEntitiesChannel1));
      Assertions.assertEquals(conduit.allEntities().size(), 1000);

    }
    for (Conduit conduit : conduits2) {
      Assertions.assertTrue(conduit.allEntities().containsAll(allEntitiesChannel2));
      Assertions.assertEquals(conduit.allEntities().size(), 1000);
    }
    Assertions.assertEquals(0, threadError.get());
  }
}
