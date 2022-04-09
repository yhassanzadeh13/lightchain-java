package network.p2p;

import java.io.IOException;
import java.nio.ByteBuffer;
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

/**
 * Implements a grpc-based networking layer.
 */
public class P2pNetwork implements network.Network {
  public int NETWORK_SERVER_PORT;
  MessageServer server;

  public P2pNetwork() {
    this(1);
  }

  public P2pNetwork(int port) {
    NETWORK_SERVER_PORT = port;
    server = new MessageServer(NETWORK_SERVER_PORT);
  }

  public void start() {
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

    if (server.engineChannelTable.containsKey(channel)) {
      throw new IllegalStateException("channel already exist");
    }

    P2pConduit conduit = new P2pConduit(this, e);
    server.engineChannelTable.put(channel, e);

    return conduit;

  }

  public void sendUnicast(Entity e, Identifier target, Engine sourceEngine) throws InterruptedException, IOException {

    // target will be obtained from identifier when its implemented

    String targetServer = String.valueOf(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(target.getBytes())));
    System.out.println("Target: " + targetServer);

    ManagedChannel managedChannel = ManagedChannelBuilder.forTarget(targetServer).usePlaintext().build();

    // find channel of the source engine

    String channel="";

    for (String c : server.engineChannelTable.keySet()) {
      if (server.engineChannelTable.get(c).equals(sourceEngine)) channel = c;
    }

    try {
      MessageClient client = new MessageClient(managedChannel);
      client.deliver(e, target, channel);
    } finally {
      managedChannel.shutdownNow();
    }

  }

}
