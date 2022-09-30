package bootstrap;

/**
 * Runs the bootstrap routine to initialize identities for a LightChain network.
 */
public class Cmd {

  /**
   * Number of nodes to be created.
   */
  private static final short nodeCount = 10;

  /**
   * Main method.
   *
   * @param args command-line arguments.
   */
  public static void main(String[] args) {
    Bootstrap bootstrap = new Bootstrap(nodeCount);
    bootstrap.build();
  }
}
