package bootstrap;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import model.exceptions.LightChainNetworkingException;
import model.lightchain.Identifier;
import modules.logger.LightchainLogger;
import modules.logger.Logger;
import network.Conduit;
import network.p2p.P2pNetwork;
import protocol.Engine;

/**
 * Node class representing the implementation of a singular structure capable of utilizing different engines, which
 * may use Networks in order to transmit and receive Entities amongst themselves.
 */
public class Node {
  static ConcurrentMap<Identifier, String> idTable;
  static Identifier myId;
  static P2pNetwork network;
  static Engine engine;
  static Conduit conduit;
  static final Logger logger = LightchainLogger.getLogger(Node.class.getCanonicalName());

  /**
   * Main method.
   *
   * @param args Arguments.
   */
  public static void main(String[] args) throws InterruptedException {
    myId = new Identifier(args[0].getBytes(StandardCharsets.UTF_8));
    idTable = readFromOutput(args[1]);
    network = new P2pNetwork(myId, 8081);
    network.setIdToAddressMap(idTable);
    // TODO change this from hardcoded.
    engine = new BroadcastEngine(idTable, myId);
    conduit = network.register(engine, "channel1");

    try {
      network.start();
    } catch (IOException e) {
      logger.fatal("could not start network", e);
    }

    // converts the idTable to a string and prints it.
    String idTableStr = idTable.entrySet()
        .stream()
        .map(Map.Entry::toString)
        .collect(Collectors.joining(",", "[", "]"));

    logger.info("node {} started successfully at address {}, bootstrap table {}", myId, network.getAddress(), idTableStr);
    sendHelloMessagesToAll(1000);
  }

  /**
   * Sends a hello message to all nodes in the network.
   */
  private static void sendHelloMessagesToAll(int count) {
    for(int i = 0; i < count; i++) {
      try {
        TimeUnit.MILLISECONDS.sleep(1000);
      } catch (InterruptedException e) {
        logger.fatal("could not sleep", e);
      }

      for (Map.Entry<Identifier, String> id : idTable.entrySet()) {
        if (!id.getKey().toString().equals(myId.toString())) {
          HelloMessageEntity e = new HelloMessageEntity("# + " + i+1 + " Hello from " + myId + " to " + id.getKey());
          try {
            conduit.unicast(e, id.getKey());
          } catch (LightChainNetworkingException ex) {
            logger.fatal("could not send hello message", ex);
          }
        }
      }
    }
  }



  /**
   * Reads the given path and returns a ConcurrentMap of Identifiers to IP addresses.
   *
   * @param path Path to the file.
   * @return A ConcurrentMap of Identifiers to IP addresses.
   */
  private static ConcurrentMap<Identifier, String> readFromOutput(String path) {
    ConcurrentMap<Identifier, String> map = new ConcurrentHashMap<>();

    try {
      File idTableFile = new File(path);
      InputStream inputStream = new FileInputStream(idTableFile);
      Reader readerStream = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
      BufferedReader reader = new BufferedReader(readerStream);
      String line;

      while ((line = reader.readLine()) != null) {
        String[] strings = line.split(":");
        // This is potentially to be reconsidered if there is a way to pass Identifiers directly.
        map.put(new Identifier(strings[1].getBytes(StandardCharsets.UTF_8)), strings[1] + ":" + strings[2]);
      }
      reader.close();
    } catch (FileNotFoundException e) {
      logger.fatal("could not find file", e);
    } catch (IOException e) {
      logger.fatal("could not read file", e);
    }
    return map;
  }
}
