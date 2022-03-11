package networking;

import java.util.ArrayList;
import java.util.Hashtable;

import model.Entity;
import model.exceptions.LightChainNetworkingException;
import network.Conduit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import protocol.Engine;
import unittest.fixtures.EntityFixture;

public class StubNetworkTest {
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

  @Test
  void TestTwoStubNetworks_TwoEngines(){
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


//  @Test
//  void test() throws LightChainNetworkingException {
//
//    String channelA1 = "ChannelA1";
//
//    Engine A1 = new MockEngine();
//    Engine B1 = new MockEngine();
//    stubNetwork1.register(A1, channelA1);
//    stubNetwork2.register(B1, channelA1);
//    EntityFixture e1 = new EntityFixture();
//    EntityFixture e2 = new EntityFixture();
//    StubNetworkThread t1 = new StubNetworkThread(stubNetwork1, stubNetwork2, e1, channelA1);
//    t1.start();
//    //c1.unicast(e1, stubNetwork2.id());
//    System.out.println(e1.id());
//    System.out.println(B1);
//  }
//
//  @Test
//  void TestUnicastOneToAll_Sequentially() throws LightChainNetworkingException {
//    Conduit c1 = stubNetwork1.register(A1, channel1);
//    EntityFixture e1 = new EntityFixture();
//
//    stubNetwork1.register(A2, channel2);
//    stubNetwork2.register(B1, channel1);
//    stubNetwork2.register(B2, channel2);
//    stubNetwork3.register(C1, channel1);
//    stubNetwork3.register(C2, channel2);
//    stubNetwork4.register(D1, channel1);
//    stubNetwork4.register(D2, channel2);
//    stubNetwork5.register(E1, channel1);
//    stubNetwork5.register(E2, channel2);
//    stubNetwork6.register(F1, channel1);
//    stubNetwork6.register(F2, channel2);
//    stubNetwork7.register(G1, channel1);
//    stubNetwork7.register(G2, channel2);
//    stubNetwork8.register(H1, channel1);
//    stubNetwork8.register(H2, channel2);
//    stubNetwork9.register(I1, channel1);
//    stubNetwork9.register(I2, channel2);
//    stubNetwork10.register(J1, channel1);
//    stubNetwork10.register(J2, channel2);
//    c1.unicast(e1, stubNetwork2.id());
//    c1.unicast(e1, stubNetwork3.id());
//    c1.unicast(e1, stubNetwork4.id());
//    c1.unicast(e1, stubNetwork5.id());
//    c1.unicast(e1, stubNetwork6.id());
//    c1.unicast(e1, stubNetwork7.id());
//    c1.unicast(e1, stubNetwork8.id());
//    c1.unicast(e1, stubNetwork9.id());
//    c1.unicast(e1, stubNetwork10.id());
//    System.out.println(e1.id());
//    System.out.println(B1);
//    System.out.println(B2);
//    System.out.println(C1);
//    System.out.println(C2);
//    System.out.println(D1);
//    System.out.println(D2);
//
//
//  }
//
//  @Test
//  void TestUnicastOneToAll_Concurrently() {
//
//    EntityFixture e1 = new EntityFixture();
//    stubNetwork1.register(A1, channel1);
//    stubNetwork1.register(A2, channel2);
//    stubNetwork2.register(B1, channel1);
//    stubNetwork2.register(B2, channel2);
//    stubNetwork3.register(C1, channel1);
//    stubNetwork3.register(C2, channel2);
//    stubNetwork4.register(D1, channel1);
//    stubNetwork4.register(D2, channel2);
//    stubNetwork5.register(E1, channel1);
//    stubNetwork5.register(E2, channel2);
//    stubNetwork6.register(F1, channel1);
//    stubNetwork6.register(F2, channel2);
//    stubNetwork7.register(G1, channel1);
//    stubNetwork7.register(G2, channel2);
//    stubNetwork8.register(H1, channel1);
//    stubNetwork8.register(H2, channel2);
//    stubNetwork9.register(I1, channel1);
//    stubNetwork9.register(I2, channel2);
//    stubNetwork10.register(J1, channel1);
//    stubNetwork10.register(J2, channel2);
//    StubNetworkThread t1 = new StubNetworkThread(stubNetwork1, stubNetwork2, e1, channel1);
//    StubNetworkThread t2 = new StubNetworkThread(stubNetwork1, stubNetwork3, e1, channel1);
//    StubNetworkThread t3 = new StubNetworkThread(stubNetwork1, stubNetwork4, e1, channel1);
//    StubNetworkThread t4 = new StubNetworkThread(stubNetwork1, stubNetwork5, e1, channel1);
//    StubNetworkThread t5 = new StubNetworkThread(stubNetwork1, stubNetwork6, e1, channel1);
//    StubNetworkThread t6 = new StubNetworkThread(stubNetwork1, stubNetwork7, e1, channel1);
//    StubNetworkThread t7 = new StubNetworkThread(stubNetwork1, stubNetwork8, e1, channel1);
//    StubNetworkThread t8 = new StubNetworkThread(stubNetwork1, stubNetwork9, e1, channel1);
//    StubNetworkThread t9 = new StubNetworkThread(stubNetwork1, stubNetwork10, e1, channel1);
//
//    t1.start();
//    t2.start();
//    t3.start();
//    t4.start();
//    t5.start();
//    t6.start();
//    t7.start();
//    t8.start();
//    t9.start();
//    System.out.println(e1.id());
//    System.out.println(B1);
//    System.out.println(B2);
//    System.out.println(C1);
//    System.out.println(C2);
//    System.out.println(D1);
//    System.out.println(D2);
//
//  }
}