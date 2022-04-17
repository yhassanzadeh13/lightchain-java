package bootstrap;

import integration.localnet.LocalTestNet;
import model.lightchain.Identifier;

import java.util.HashMap;

public class Bootstrap {



  public static void main(String[] args) {

    int nodeCount = 2;
    HashMap<Identifier,String> idTable;

    LocalTestNet testNet = new LocalTestNet(nodeCount);

    try {
      idTable = testNet.createNodeContainers();
      for (Identifier id : idTable.keySet()) {
        System.out.println(id.toString());
      }
    } catch (IllegalStateException e) {
      System.err.println("could not initialize and run node net: " + e.getMessage());
      System.exit(1);
    }



  }

}
