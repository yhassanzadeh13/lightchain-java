package network.p2p;

import java.io.IOException;
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

/**
 * Implements a grpc-based networking layer.
 */
public class P2pNetwork implements network.Network {
  private static HashMap<String, Engine> engineChannelTable;
  private static int NETWORK_SERVER_PORT;

  public P2pNetwork() {
    this.engineChannelTable = new HashMap<String, Engine>();
  }

  public static void main(String[] args) {

    NETWORK_SERVER_PORT = 8980;

    MessageServer server = new MessageServer(NETWORK_SERVER_PORT);
    try {
      server.start();
      server.blockUntilShutdown();
    } catch (Exception e) {
      System.out.println("LightChain Network has failed during the transmission ");
      e.printStackTrace();
    }
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
    engineChannelTable.put(channel, e);

    return conduit;

  }

  public void sendUnicast(Entity e, Identifier target, Engine sourceEngine) throws InterruptedException, IOException {

    String channel;

    for (String c : engineChannelTable.keySet()) {
      if (engineChannelTable.get(c).equals(sourceEngine)) channel = c;
    }

    // target will be obtained from identifier when its implemented

    String targetServer = "localhost:8980";
    ManagedChannel managedChannel = ManagedChannelBuilder.forTarget(targetServer).usePlaintext().build();

    try {
      MessageClient client = new MessageClient(managedChannel);
      client.deliver(e, target, sourceEngine);
    } finally {
      managedChannel.shutdownNow();
    }

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

            //engineChannelTable.get(s.toStringUtf8()).process(message.getPayload());

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
