package networking.p2p;

import protocol.Engine;

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


}
