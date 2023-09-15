package network.p2p;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
   * Identifier of the lightchain node itself.
   */
  private final Identifier myId;
  /**
   * Translates identifier of nodes to their networking address.
   */
  private ConcurrentMap<Identifier, String> idToAddressMap;

  /**
   * Creates P2P network for lightchain node.
   *
   * @param myId identifier of lightchain node.
   * @param port port number of lightchain node.
   */
  public P2pNetwork(Identifier myId, int port) {
    this.server = new MessageServer(port);
    this.idToAddressMap = new ConcurrentHashMap<>();
    this.myId = myId;
  }

  /**
   * Starts the MessageServer and waits until it is shutdown.
   */
  public void start() throws IllegalStateException {
    try {
      this.server.start();
    } catch (IOException e) {
      throw new IllegalStateException("failed to start server", e);
    }
  }

  public void stop() throws InterruptedException {
    this.server.stop();
  }

  /**
   * Identifier of the node.
   *
   * @return identifier of the node.
   */
  public Identifier getId() {
    return myId;
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
   * @param e       the entity to be sent.
   * @param target  identifier of target node.
   * @param channel the network channel on which this entity is sent.
   * @throws InterruptedException     if the transmission of Entity relay is interrupted.
   * @throws IOException              if the gRPC channel cannot be built.
   * @throws IllegalArgumentException if target identifier does not correspond to a valid address.
   */
  public void sendUnicast(Entity e, Identifier target, String channel) throws InterruptedException, IOException, IllegalArgumentException {

    String targetAddress = this.idToAddressMap.get(target);
    if (targetAddress == null) {
      throw new IllegalArgumentException("target identifier does not exist: " + target.toString());
    }
    ManagedChannel managedChannel = ManagedChannelBuilder.forTarget(targetAddress).usePlaintext().build();
    try {
      MessageClient client = new MessageClient(managedChannel);
      client.deliver(e, target, channel);
    } finally {
      managedChannel.shutdownNow();
    }
  }

  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "internal representation is intentionally returned")
  public ConcurrentMap<Identifier, String> getIdToAddressMap() {
    return this.idToAddressMap;
  }

  /**
   * Sets idToAddressMap for this network.
   *
   * @param idToAddressMap map from identifiers to addresses.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "intentionally mutable externally")
  public void setIdToAddressMap(Map<Identifier, String> idToAddressMap) {
    this.idToAddressMap = new ConcurrentHashMap<>(idToAddressMap);
  }
}