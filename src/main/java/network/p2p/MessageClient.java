package network.p2p;


import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Empty;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessageClient {


  private static final Logger logger = Logger.getLogger(MessageClient.class.getName());

  private final MessengerGrpc.MessengerBlockingStub blockingStub;
  private final MessengerGrpc.MessengerStub asyncStub;

  /** Construct client for accessing RouteGuide server using the existing channel. */
  public MessageClient(Channel channel) {
    blockingStub = MessengerGrpc.newBlockingStub(channel);
    asyncStub = MessengerGrpc.newStub(channel);
  }

  /**
   * Async client-streaming example. Sends {@code numMessages} randomly chosen points from {@code
   * features} with a variable delay in between. Prints the statistics when they are sent from the
   * server.
   */
  public void deliver(int numMessages) throws InterruptedException {
    info("*** deliver");
    final CountDownLatch finishLatch = new CountDownLatch(1);
    StreamObserver<Empty> responseObserver = new StreamObserver<Empty>() {

      @Override
      public void onNext(Empty empty) {

      }

      @Override
      public void onError(Throwable t) {
        warning("deliver Failed: {0}", Status.fromThrowable(t));
        finishLatch.countDown();
      }

      @Override
      public void onCompleted() {
        info("Finished deliver");
        finishLatch.countDown();
      }
    };

    StreamObserver<Message> requestObserver = asyncStub.deliver(responseObserver);
    try {
      // Send numMessages points randomly selected from the features list.
      for (int i = 0; i < numMessages; ++i) {
        System.out.println("here " + i);
        Message message = Message.newBuilder().build();
        requestObserver.onNext(message);
        // Sleep for a bit before sending the next one.
        Thread.sleep( 1000);
        if (finishLatch.getCount() == 0) {
          // RPC completed or errored before we finished sending.
          // Sending further requests won't error, but they will just be thrown away.
          return;
        }
      }
    } catch (RuntimeException e) {
      // Cancel RPC
      requestObserver.onError(e);
      throw e;
    }
    // Mark the end of requests
    requestObserver.onCompleted();

    // Receiving happens asynchronously
    if (!finishLatch.await(1, TimeUnit.MINUTES)) {
      warning("deliver can not finish within 1 minutes");
    }
  }

  private void info(String msg, Object... params) {
    logger.log(Level.INFO, msg, params);
  }

  private void warning(String msg, Object... params) {
    logger.log(Level.WARNING, msg, params);
  }

}
