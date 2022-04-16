package bootstrap;

import integration.localnet.LocalTestNet;

public class Bootstrap {



  public static void main(String[] args) {

    int nodeCount = 10;

    LocalTestNet testNet = new LocalTestNet(nodeCount);

    try {
      testNet.createNodeContainers();
    } catch (IllegalStateException e) {
      System.err.println("could not initialize and run node net: " + e.getMessage());
      System.exit(1);
    }
  }

}
