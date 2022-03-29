package integration.localnet;

/**
 * The main runner class for orchestrating the building of the HTTP Server, Prometheus, and Grafana
 * components of the TestNet. This class also utilizes the Java Docker API in order to containerize
 * the three components and build up the necessary supporting infrastructure.
 */
public class Main {
  /**
   * The main function.
   *
   * @param args standard java parameters.
   */
  public static void main(String[] args) {
    LocalTestNet testNet = new LocalTestNet();

    try {
      testNet.runLocalTestNet();
    } catch (IllegalStateException e) {
      System.err.println("could not initialize and run HTTP Server Container" + e);
      System.exit(1);
    }
  }
}
