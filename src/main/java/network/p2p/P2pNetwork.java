package network.p2p;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import model.Entity;
import model.lightchain.Identifier;
import network.Conduit;
import protocol.Engine;

/**
 * Implements a grpc-based networking layer.
 */
public class P2pNetwork implements network.Network {
  private HashMap engineChannelTable;

  public P2pNetwork() throws IOException, InterruptedException {
    this.engineChannelTable = new HashMap<Engine, String>();

  }

  public static void main(String[] args) throws IOException, InterruptedException {

    MessageServer server = new MessageServer(8980);
    server.start();

    // START

    String targetServer = "localhost:8980";
    ManagedChannel channel = ManagedChannelBuilder.forTarget(targetServer).usePlaintext().build();

    try {
      MessageClient client = new MessageClient(channel);

      // Record a few randomly selected points from the features file.
      client.deliver(7);

    } finally {
      channel.shutdownNow();
    }

    // END

    server.blockUntilShutdown();



    System.out.println("yoo");
  }

  /**
   * Registers an Engine to the Network by providing it with a Conduit.
   *
   * @param e       the Engine to be registered.
   * @param channel the unique channel corresponding to the Engine.
   * @return unique Conduit object created to connect the Network to the Engine.
   * @throws IllegalStateException if the channel is already taken by another Engine.
   */
  @Override
  public Conduit register(Engine e, String channel) throws IllegalStateException {

    if (engineChannelTable.containsKey(channel)) {
      throw new IllegalStateException("channel already exist");
    }

    P2pConduit conduit = new P2pConduit(this);
    engineChannelTable.put(e, channel);

    return conduit;

  }

  public void sendUnicast(Entity e, Identifier target) throws InterruptedException {

    // get the target ip:port from the entity e

    e.id();

    // relay the message by using the appropriate gRPC method



  }

  public static class MessengerImpl extends MessengerGrpc.MessengerImplBase {
    private final Logger logger = Logger.getLogger(MessengerImpl.class.getName());
    @Override
    public StreamObserver<Message> deliver(StreamObserver<Empty> responseObserver) {

      return new StreamObserver<Message>() {

        @Override
        public void onNext(Message message) {

        }

        @Override
        public void onError(Throwable t) {
          logger.log(Level.WARNING, "Encountered error in deliver", t);
        }

        @Override
        public void onCompleted() {
          System.out.println("h");
          responseObserver.onCompleted();
        }
      };

    }
  }

}
