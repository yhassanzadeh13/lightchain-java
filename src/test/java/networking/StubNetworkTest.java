package networking;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

public class StubNetworkTest {


    private ArrayList<Network> networkArrayList;
    private final String channel1 = "test-network-channel-1";
    private final String channel2 = "test-network-channel-2";
    private Hub hub;

    // TODO: add a test for each of the following scenarios:
    // Use mock engines.
    // 1. Engine A (on one stub network) can send message to Engine B (on another stub network) through its StubNetwork, and the message is received by Engine B.
    // 2. Engine A can CONCURRENTLY send 100 messages to Engine B through its StubNetwork, and ALL messages received by Engine B.
    // 3. Extend case 2 with Engine B also sending a reply message to Engine A for each received messages and all replies
    // are received by Engine A.
    // 4. Engines A and B on one StubNetwork can CONCURRENTLY send 100 messages to Engines C and D on another StubNetwork (A -> C) and (B -> D), and each Engine only
    // receives messages destinated for it (C receives all messages from A) and (D receives all messages from B). Note that A and C must be on the same channel, and B
    // and B must be on another same channel.
    // 5. Stub network throws an exception if an engine is registering itself on an already taken channel.
    @BeforeEach
    void setup() {


        this.networkArrayList = new ArrayList<>();
        hub = new Hub();
        for (int i = 0; i < 9; i++) {

            StubNetwork stubNetwork = new StubNetwork(hub);
            Engine E1 = new MockEngine();
            Engine E2 = new MockEngine();
            stubNetwork.register(E1, channel1);
            stubNetwork.register(E2, channel2);
            networkArrayList.add(stubNetwork);


        }

    }

    @Test
    void TestTwoStubNetworks_TwoEngines() {
        String channel1 = "test-network-channel-1";
        Hub hub = new Hub();

        StubNetwork network1 = new StubNetwork(hub);
        MockEngine A1 = new MockEngine();
        Conduit c1 = network1.register(A1, channel1);

        StubNetwork network2 = new StubNetwork(hub);
        MockEngine A2 = new MockEngine();
        network2.register(A2, channel1);


        Entity entity = new EntityFixture();

        try {
            c1.unicast(entity, network2.id());
        } catch (LightChainNetworkingException e) {
            Assertions.fail();
        }

        Assertions.assertTrue(A2.hasReceived(entity));
    }

    @Test
    void TestTwoStubNetworks_TwoEngines_ConcurrentMessages() {
        int concurrencyDegree = 100;
        AtomicInteger threadError = new AtomicInteger();
        CountDownLatch countDownLatch = new CountDownLatch(concurrencyDegree);

        String channel1 = "test-network-channel-1";

        Thread[] unicastThreads = new Thread[concurrencyDegree];


        StubNetwork network1 = new StubNetwork(hub);
        MockEngine A1 = new MockEngine();
        Conduit c1 = network1.register(A1, channel1);

        StubNetwork network2 = new StubNetwork(hub);
        MockEngine A2 = new MockEngine();
        network2.register(A2, channel1);


        for (int i = 0; i < concurrencyDegree; i++) {
            unicastThreads[i] = new Thread(() -> {
                Entity entity = new EntityFixture();

                try {
                    c1.unicast(entity, network2.id());
                    if (!A2.hasReceived(entity)) {
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

    @Test
    void TestUnicastOneToAll_Concurrently() {
        int concurrencyDegree = 9;
        AtomicInteger threadError = new AtomicInteger();
        CountDownLatch countDownLatch = new CountDownLatch(concurrencyDegree);
        StubNetwork network1 = new StubNetwork(hub);
        MockEngine A1 = new MockEngine();
        Conduit c1 = network1.register(A1, channel1);
        Entity entity = new EntityFixture();


        Thread[] unicastThreads = new Thread[concurrencyDegree];
        int count = 0;
        for (Network network : networkArrayList) {
            unicastThreads[count] = new Thread(() -> {


                try {


                    c1.unicast(entity, ((StubNetwork) network).id());
                    MockEngine E1 = (MockEngine) ((StubNetwork) network).getEngine(channel1);
                    MockEngine E2 = (MockEngine) ((StubNetwork) network).getEngine(channel2);

                    if (!E1.hasReceived(entity)) {
                        threadError.getAndIncrement();
                    }
                    if (E2.hasReceived(entity)) {
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


}