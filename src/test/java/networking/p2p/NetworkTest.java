package networking.p2p;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import model.Entity;
import model.exceptions.LightChainNetworkingException;
import model.lightchain.Identifier;
import network.Conduit;
import network.Network;
import network.p2p.P2pConduit;
import network.p2p.P2pNetwork;
import networking.MockEngine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import protocol.Engine;
import protocol.engines.IngestEngine;
import unittest.fixtures.EntityFixture;

/**
 * Encapsulates tests for gRPC implementation of the networking layer.
 */
public class NetworkTest {
  // TODO: implement test scenarios
  // Use mock engines but real p2p implemented network using gRPC
  // 1. Engine A1 (on one network) can send message to Engine A2
  //    (on another network), and the message is received by Engine A2.
  // 2. Engine A1 can CONCURRENTLY send 100 messages to Engine A2, and ALL messages received by Engine A2.
  // 3. Extend case 2 with Engine A2 also sending a reply message to Engine A1 for each received messages and all
  //    replies are received by Engine A1.
  // 4. Engines A1 and B1 on one network can CONCURRENTLY send 100 messages to Engines A2 and B2 on another network
  //    (A1 -> A2) and (B1 -> B2), and each Engine only receives messages destinated for it (A2 receives all messages
  //    from A1) and (B2 receives all messages from B1). Note that A1 and A2 must be on the same channel, and B1
  //    and B2 must be on another same channel.
  // 5. The p2p network throws an exception if an engine is registering itself on an already taken channel.


  private ArrayList<Network> networkArrayList;
  private final String channel1 = "test-network-channel-1";
  private final String channel2 = "test-network-channel-2";


//  /**
//   * Implement  before each test.
//   */
//  @BeforeEach
//  void setup() {
//    this.networkArrayList = new ArrayList<>();
//    for (int i = 0; i < 9; i++) {
//      StubNetwork stubNetwork = new StubNetwork(hub);
//      Engine e1 = new MockEngine();
//      Engine e2 = new MockEngine();
//      stubNetwork.register(e1, channel1);
//      stubNetwork.register(e2, channel2);
//      networkArrayList.add(stubNetwork);
//    }
//  }

  /**
   * Test two stub networks with two engines.
   */
  @Test
  void testTwoP2PNetworksTwoEngines() {
    P2pNetwork network1 = new P2pNetwork(1);
    MockEngine a1 = new MockEngine();
    Conduit c1 = network1.register(a1, channel1);
    P2pNetwork network2 = new P2pNetwork(2);
    MockEngine a2 = new MockEngine();
    network2.register(a2, channel1);
    Entity entity = new EntityFixture();

    CountDownLatch countDownLatch = new CountDownLatch(2);

    Thread n1Thread = new Thread(() -> {
      network1.start();
      countDownLatch.countDown();
    });

    Thread n2Thread = new Thread(() -> {
      network2.start();
      countDownLatch.countDown();
    });

    n1Thread.start();
    n2Thread.start();

    try {
      c1.unicast(entity, new Identifier(new String("localhost:"+network2.NETWORK_SERVER_PORT).getBytes(StandardCharsets.UTF_8)));
    } catch (LightChainNetworkingException e) {
      Assertions.fail();
    }
    Assertions.assertTrue(a2.hasReceived(entity));
  }


  public static void main(String[] args) throws InterruptedException {

    // start server thread

    Thread t = new Thread(() -> {
        P2pNetwork n = new P2pNetwork(1);
        n.start();
    });

    // set up mock values for testing gRPC communication.

    Thread t2 = new Thread(() -> {

      Engine sourceEngine = new MockEngine();
      Identifier target = new Identifier(new String("localhost:1").getBytes(StandardCharsets.UTF_8));
      Entity entity = new Entity() {
        @Override
        public String type() {
          return new String("type");
        }
      };

      P2pNetwork network = new P2pNetwork(2);

      Conduit conduit = network.register(sourceEngine, "BBC");

      try {
        for (int i = 0; i<100;i++) conduit.unicast(entity, target);
      } catch (LightChainNetworkingException e) {
        System.out.println("LightChain Network has failed during the transmission of " + e.toString()
                + " to " + StandardCharsets.UTF_8.decode(ByteBuffer.wrap(target.getBytes())));
        e.printStackTrace();
      }
    });

    t.start();
    TimeUnit.SECONDS.sleep(1);
    t2.start();

  }

}
