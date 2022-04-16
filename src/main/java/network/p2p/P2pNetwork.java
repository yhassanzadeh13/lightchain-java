package network.p2p;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import model.Entity;
import model.lightchain.Identifier;
import network.Conduit;
import protocol.Engine;

/**
 * Implements a grpc-based networking layer.
 */
public class P2pNetwork implements network.Network {
  private final MessageServer server;

  /**
   * Translates identifier of nodes to their networking address.
   */
  private HashMap<Identifier, String> idToAddressMap;

  public P2pNetwork(int port) {
    server = new MessageServer(port);
  }

  /**
   * Starts the MessageServer and waits until it is shutdown.
   */
  public void start() throws IOException {
    this.server.start();
  }

  public void stop() throws InterruptedException {
    this.server.stop();
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
    server.setEngine(channel, e);

    return new P2pConduit(this, channel);
  }

  public int getPort() {
    return this.server.getPort();
  }

  public String getAddress() {
    return "localhost:" + this.getPort();
  }

  /**
   * Sends the provided entity to the target P2pNetwork on a specific channel by building a gRPC ManagedServer.
   *
   * @param e      the entity to be sent.
   * @param target identifier of target node.
   * @param channel the network channel on which this entity is sent.
   * @throws InterruptedException if the transmission of Entity relay is interrupted.
   * @throws IOException          if the gRPC channel cannot be built.
   */
  public void sendUnicast(Entity e, Identifier target, String channel) throws InterruptedException, IOException {
    // target will be obtained from identifier when its implemented
    String targetServer = String.valueOf(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(target.getBytes())));

    ManagedChannel managedChannel = ManagedChannelBuilder.forTarget(targetServer).usePlaintext().build();

    try {
      MessageClient client = new MessageClient(managedChannel);
      client.deliver(e, target, channel);
    } finally {
      managedChannel.shutdownNow();
    }

  }

}
