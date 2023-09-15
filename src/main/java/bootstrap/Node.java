package bootstrap;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

import metrics.collectors.MetricServer;
import model.lightchain.Identifier;
import modules.logger.LightchainLogger;
import modules.logger.Logger;
import network.p2p.P2pNetwork;
import protocol.Engine;

/**
 * Node class representing the implementation of a singular structure capable of utilizing different engines, which
 * may use Networks in order to transmit and receive Entities amongst themselves.
 */
public class Node {
  private static final Logger logger = LightchainLogger.getLogger(Node.class.getCanonicalName());
  private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(5);
  private static Map<Identifier, String> idTable;
  private static Identifier myId;
  private static P2pNetwork network;
  private static Engine engine;

  /**
   * Main method.
   *
   * @param args cmd arguments.
   */
  public static void main(String[] args) {
    myId = new Identifier(args[0]);
    try {
      idTable = Bootstrap.readFile(args[1]);
    } catch (IOException e) {
      logger.fatal("could not read bootstrap file", e);
    }

    logger.info("starting node {} with bootstrap file {}", myId, args[1]);

    network = new P2pNetwork(myId, Bootstrap.bootstrapPortNumber);
    network.setIdToAddressMap(idTable);
    MetricServer metricServer = new MetricServer();
    DemoCollector collector = new DemoCollector();

    try {
      metricServer.start();
    } catch (IllegalStateException ex) {
      logger.fatal("could not start metric server", ex);
    }

    engine = new BroadcastEngine(idTable, myId, network, collector);

    try {
      engine.start(STARTUP_TIMEOUT);
    } catch (IllegalStateException e) {
      logger.fatal("could not broadcast engine", e);
    }

    // converts the idTable to a string and prints it.
    String idTableStr = idTable.entrySet()
        .stream()
        .map(entry -> entry.getKey().toString() + "=" + entry.getValue().toString())
        .collect(Collectors.joining(",", "[", "]"));

    logger.info("node {} started successfully at address {}, bootstrap table {}", myId, network.getAddress(), idTableStr);

    // TODO: add a shutdown hook to stop the engine and network and metric server
  }
}
