package networking;

/**
 * Encapsulates tests for stub network implementation.
 */
public class StubNetworkTest {
  // TODO: add a test for each of the following scenarios:
  // Use mock engines.
  // 1. Engine A (on one stub network) can send message to Engine B (on another stub network) through its StubNetwork,
  //    and the message is received by Engine B.
  // 2. Engine A can CONCURRENTLY send 100 messages to Engine B through its StubNetwork,
  //    and ALL messages received by Engine B.
  // 3. Extend case 2 with Engine B also sending a reply message to Engine A for each received messages and all replies
  // are received by Engine A.
  // 4. Engines A and B on one StubNetwork can CONCURRENTLY send 100 messages to Engines C and D on another StubNetwork
  //    (A -> C) and (B -> D), and each Engine only
  //    receives messages destinated for it (C receives all messages from A) and (D receives all messages from B).
  //    Note that A and C must be on the same channel, and B and B must be on another same channel.
  // 5. Stub network throws an exception if an engine is registering itself on an already taken channel.
}
