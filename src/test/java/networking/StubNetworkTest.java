package networking;

import model.exceptions.LightChainNetworkingException;
import network.Conduit;
import network.Network;
import org.junit.jupiter.api.Test;
import protocol.Engine;
import unittest.fixtures.EntityFixture;

import java.util.Iterator;

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
    Hub hub = new Hub();
    StubNetwork stubNetwork1 = new StubNetwork(hub);
    StubNetwork stubNetwork2 = new StubNetwork(hub);
    StubNetwork stubNetwork3 = new StubNetwork(hub);
    StubNetwork stubNetwork4 = new StubNetwork(hub);
    StubNetwork stubNetwork5 = new StubNetwork(hub);
    StubNetwork stubNetwork6 = new StubNetwork(hub);
    StubNetwork stubNetwork7 = new StubNetwork(hub);
    StubNetwork stubNetwork8 = new StubNetwork(hub);
    StubNetwork stubNetwork9 = new StubNetwork(hub);
    StubNetwork stubNetwork10 = new StubNetwork(hub);
    String channel1="test-network-channel-1";
    String channel2="test-network-channel-2";
    Engine A1 = new MockEngine();
    Engine A2 = new MockEngine();
    Engine B1 = new MockEngine();
    Engine B2 = new MockEngine();
    Engine C1 = new MockEngine();
    Engine C2 = new MockEngine();
    Engine D1 = new MockEngine();
    Engine D2 = new MockEngine();
    Engine E1 = new MockEngine();
    Engine E2 = new MockEngine();
    Engine F1 = new MockEngine();
    Engine F2 = new MockEngine();
    Engine G1 = new MockEngine();
    Engine G2 = new MockEngine();
    Engine H1 = new MockEngine();
    Engine H2 = new MockEngine();
    Engine I1 = new MockEngine();
    Engine I2 = new MockEngine();
    Engine J1 = new MockEngine();
    Engine J2 = new MockEngine();


    @Test

    void test() throws LightChainNetworkingException {

        String channelA1="ChannelA1";

        Engine A1 = new MockEngine();
        Engine B1 = new MockEngine();
        stubNetwork1.register(A1, channelA1);
        stubNetwork2.register(B1,channelA1);
        EntityFixture e1 = new EntityFixture();
        EntityFixture e2 = new EntityFixture();
        StubNetworkThread t1 = new StubNetworkThread(stubNetwork1,stubNetwork2,e1,channelA1);
        t1.start();
        //c1.unicast(e1, stubNetwork2.id());
        System.out.println(e1.id());
        System.out.println(B1);
    }

    @Test
    void TestUnicastOneToAll_Sequentially() throws LightChainNetworkingException {
        Conduit c1 =stubNetwork1.register(A1,channel1);
        EntityFixture e1 =new EntityFixture();

        stubNetwork1.register(A2,channel2);
        stubNetwork2.register(B1,channel1);
        stubNetwork2.register(B2,channel2);
        stubNetwork3.register(C1,channel1);
        stubNetwork3.register(C2,channel2);
        stubNetwork4.register(D1,channel1);
        stubNetwork4.register(D2,channel2);
        stubNetwork5.register(E1,channel1);
        stubNetwork5.register(E2,channel2);
        stubNetwork6.register(F1,channel1);
        stubNetwork6.register(F2,channel2);
        stubNetwork7.register(G1,channel1);
        stubNetwork7.register(G2,channel2);
        stubNetwork8.register(H1,channel1);
        stubNetwork8.register(H2,channel2);
        stubNetwork9.register(I1,channel1);
        stubNetwork9.register(I2,channel2);
        stubNetwork10.register(J1,channel1);
        stubNetwork10.register(J2,channel2);
        c1.unicast(e1,stubNetwork2.id());
        c1.unicast(e1,stubNetwork3.id());
        c1.unicast(e1,stubNetwork4.id());
        c1.unicast(e1,stubNetwork5.id());
        c1.unicast(e1,stubNetwork6.id());
        c1.unicast(e1,stubNetwork7.id());
        c1.unicast(e1,stubNetwork8.id());
        c1.unicast(e1,stubNetwork9.id());
        c1.unicast(e1,stubNetwork10.id());
        System.out.println(e1.id());
        System.out.println(B1);
        System.out.println(B2);
        System.out.println(C1);
        System.out.println(C2);
        System.out.println(D1);
        System.out.println(D2);


    }
}