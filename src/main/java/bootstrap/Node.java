package bootstrap;

import model.Entity;
import model.exceptions.LightChainNetworkingException;
import model.lightchain.Identifier;
import network.Conduit;
import network.p2p.P2pConduit;
import network.p2p.P2pNetwork;
import protocol.Engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class Node {

  static ConcurrentMap<Identifier, String> idTable;
  static Identifier myId;
  static P2pNetwork network;
  static Engine engine;
  static Conduit conduit;

  public static void main(String[] args) {

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
      e.printStackTrace();
    }

    System.out.println("Hello world! I'm Node at Port: " + network.getPort());
    System.out.println("My ID is: " + myId);

    System.out.println("ID Table Database AFAIK: ");
    for (Identifier id : idTable.keySet()) {
      System.out.println(id + " " + idTable.get(id));
    }

    sendHelloMessagesToAll();

  }

  private static void sendHelloMessagesToAll() {

    while (true) {

      try {
        TimeUnit.SECONDS.sleep(1);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      for (Identifier id : idTable.keySet()) {

        if (!id.toString().equals(myId.toString())) {

          HelloMessageEntity e = new HelloMessageEntity("Hello from " + myId + " to " + id);

          try {
            conduit.unicast(e, id);
          } catch (LightChainNetworkingException ex) {
            ex.printStackTrace();
          }

        }

      }

    }

  }

  private static ConcurrentMap<Identifier, String> readFromOutput(String path) {

    ConcurrentMap<Identifier, String> map = new ConcurrentHashMap<>();

    try {

      File idTableFile = new File(path);

      BufferedReader reader = new BufferedReader(new FileReader(idTableFile));

      String line;

      while ((line = reader.readLine()) != null) {

        String[] strings = line.split(":");
        // This is potentially to be reconsidered if there is a way to pass Identifiers directly.
        map.put(new Identifier(strings[1].getBytes(StandardCharsets.UTF_8)), strings[1] + ":" + strings[2]);

      }
      reader.close();
    } catch (Exception e) {
      e.printStackTrace();
    }

    return map;

  }

}
