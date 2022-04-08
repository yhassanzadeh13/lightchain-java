package network.p2p;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import model.Entity;
import model.lightchain.Identifier;
import network.Conduit;
import protocol.Engine;
import protocol.engines.IngestEngine;

/**
 * Implements a grpc-based networking layer.
 */
public class P2pNetwork implements network.Network {
  private HashMap<Engine, String> engineChannelTable;

  public P2pNetwork() {
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

      // relay the message by using the appropriate gRPC method to remote Engines of the same channel on other networks
      Engine sourceEngine = new IngestEngine();
      Identifier target = new Identifier(new String("identifier").getBytes(StandardCharsets.UTF_8));
      Entity entity = new Entity() {
        @Override
        public String type() {
          return new String("type");
        }
      };
      client.deliver(entity, target, sourceEngine);

    } finally {
      channel.shutdownNow();
    }

    // END

    server.blockUntilShutdown();

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

    P2pConduit conduit = new P2pConduit(this, e);
    engineChannelTable.put(e, channel);

    return conduit;

  }

  public void sendUnicast(Entity e, Identifier target, Engine sourceEngine) throws InterruptedException, IOException {

    // target will be obtained from identifier when its implemented

    // the part inbetween the START and END commands will be moved here once the relevant parts are implemented

  }

  public static class MessengerImpl extends MessengerGrpc.MessengerImplBase {
    private final Logger logger = Logger.getLogger(MessengerImpl.class.getName());

    @Override
    public StreamObserver<Message> deliver(StreamObserver<Empty> responseObserver) {

      return new StreamObserver<Message>() {

        @Override
        public void onNext(Message message) {

          System.out.println("Received Entity");
          System.out.println("OriginID: " + message.getOriginId().toStringUtf8());
          System.out.println("Type: " + message.getType());

          int i = 0;
          for (ByteString s : message.getTargetIdsList()) {
            System.out.println("Target " + i++ + ": " + s.toStringUtf8());
          }

        }

        @Override
        public void onError(Throwable t) {
          logger.log(Level.WARNING, "Encountered error in deliver", t);
        }

        @Override
        public void onCompleted() {
          responseObserver.onNext(com.google.protobuf.Empty.newBuilder().build());
          responseObserver.onCompleted();
        }
      };

    }
  }

}
