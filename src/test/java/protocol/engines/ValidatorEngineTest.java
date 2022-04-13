package protocol.engines;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import model.lightchain.ValidatedBlock;
import model.local.Local;
import network.Conduit;
import network.Network;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import state.State;

/**
 * Encapsulates tests for validator engine.
 */
public class ValidatorEngineTest {
  // TODO: a single individual test function for each of these scenarios:
  // Note: except when explicitly mentioned, always assume the input block or transaction is assigned to this node
  // i.e., mock the assigner for that.
  // 1. Happy path of receiving a valid single block.
  // 2. Happy path of receiving two valid blocks sequentially.
  // 3. Happy path of receiving two valid blocks concurrently.
  // 4. Happy path of receiving two duplicate blocks sequentially (the second duplicate block should be discarded).
  // 5. Happy path of receiving two duplicate blocks concurrently (the second duplicate block should be discarded).
  // 6. Happy path of receiving a valid block with shared transactions in pendingTx.
  // 7. Happy path of receiving an already validated block,
  //    second block should be discarded right away.
  // 8.  Unhappy path of receiving a transaction and block (sequentially) that is not assigned to this node.
  // 9.  Unhappy path of receiving a transaction and block (concurrently) that is not assigned to this node.
  // 10. Happy path of receiving a valid transaction.
  // 11. Happy path of receiving two valid transactions sequentially.
  // 12. Happy path of receiving two valid transactions concurrently.
  // 13. Happy path of receiving a duplicate pair of valid transactions sequentially.
  // 14. Happy path of receiving a duplicate pair of valid transactions concurrently.
  // 15. Happy path of receiving a transaction that already been validated (second transaction should be discarded).
  // 16. Unhappy path of receiving an entity that is neither a block nor a transaction.
  // 17. Happy path of receiving a transaction and a block concurrently (block does not contain that transaction).
  // 18. Happy path of receiving a transaction and a block concurrently (block does contain the transaction).
  // 19. Unhappy path of receiving a invalid transaction (one test per each validation criteria, e.g., correctness,
  //     soundness, etc). Invalid transaction should be discarded without sending back a signature to its sender.
  // 19. Unhappy path of receiving a invalid block (one test per each validation criteria, e.g., correctness,
  //     soundness, etc). Invalid block should be discarded without sending back a signature to its proposer.

  @Test
  public void testReceiveTwoValidBlockConcurrently() {
    Network net = mock(Network.class);
    Conduit conduit = mock(Conduit.class);
    State state = mock(State.class);
    Local local = new Local();


    ValidatorEngine engine = new ValidatorEngine(net, local, state);
    when(net.register(engine, "validator")).then()

    ValidatedBlock b = null;

    AtomicInteger threadErrorCount = new AtomicInteger();
    CountDownLatch done = new CountDownLatch(1);

    
    Thread t = new Thread(() -> {
      // implement body of thread.
      // if some error happens that leads to test failure:
      // threadErrorCount.getAndIncrement();
      engine.process(b);
      done.countDown();
    });

    // run threads
    t.start();

    try {
      boolean doneOnTime = done.await(1, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOnTime);
    } catch (InterruptedException e) {
      Assertions.fail(e);
    }

    Assertions.assertEquals(0, threadErrorCount.get());


  }
}
