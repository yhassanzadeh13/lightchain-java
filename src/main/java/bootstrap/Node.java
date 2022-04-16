package bootstrap;

import model.lightchain.Identifier;
import network.p2p.P2pNetwork;

import java.io.IOException;
import java.util.HashMap;

public class Node {

  HashMap<Identifier,String> idTable;
  Identifier myId;
  static P2pNetwork network;

  public Node(HashMap<Identifier, String> idTable, Identifier myId) {
    this.idTable = idTable;
    this.myId = myId;
    this.network = new P2pNetwork(0);
    try {
      network.start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public P2pNetwork getNetwork() {
    return network;
  }

  public static void main(String[] args) {
    network = new P2pNetwork(0);
    try {
      network.start();
    } catch (IOException e) {
      e.printStackTrace();
    }
    System.out.println("Hello world! I'm Node " + network.getAddress() + " at Port: " + network.getPort());
  }

}
