package bootstrap;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import model.exceptions.LightChainNetworkingException;
import model.lightchain.Identifier;
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

  /**
   * Main method.
   *
   * @param args Arguments.
   */
  public static void main(String[] args) throws InterruptedException {
    // Set myID
    myId = new Identifier(args[0].getBytes(StandardCharsets.UTF_8));
    // Set up idTable from the given path
    idTable = readFromOutput(args[1]);
    network = new P2pNetwork(myId, 8081);
    network.setIdToAddressMap(idTable);
    // TODO change this from hardcoded.
    engine = new BroadcastEngine(idTable, myId);
    conduit = network.register(engine, "channel1");

    try {
      network.start();
    } catch (IOException e) {
      System.exit(1);
      throw new IllegalStateException("could start the network: " + e);
    }

    System.out.println("Hello world! I'm Node at Port: " + network.getPort());
    System.out.println("My ID is: " + myId);
    System.out.println("ID Table Database AFAIK: ");
    for (Map.Entry<Identifier, String> id : idTable.entrySet()) {
      System.out.println(id.getKey() + " " + idTable.get(id.getKey()));
    }
    sendHelloMessagesToAll();
  }

  /**
   * Sends a hello message to all nodes in the network.
   *
   * @throws InterruptedException if the thread is interrupted.
   */
  private static void sendHelloMessagesToAll() throws InterruptedException {
    while (true) {
      try {
        TimeUnit.MILLISECONDS.sleep(1000);
      } catch (InterruptedException e) {
        throw new InterruptedException("interrupted while sleeping");
      }

      for (Map.Entry<Identifier, String> id : idTable.entrySet()) {
        if (!id.getKey().toString().equals(myId.toString())) {
          HelloMessageEntity e = new HelloMessageEntity("Hello from " + myId + " to " + id.getKey());
          try {
            conduit.unicast(e, id.getKey());
          } catch (LightChainNetworkingException ex) {
            System.exit(1);
            throw new IllegalStateException("could not send the entity", ex);
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
      System.exit(1);
      throw new IllegalStateException("could not found the file", e);
    } catch (IOException e) {
      System.exit(1);
      throw new IllegalStateException("could not read/write from/to file", e);
    }
    return map;
  }
}