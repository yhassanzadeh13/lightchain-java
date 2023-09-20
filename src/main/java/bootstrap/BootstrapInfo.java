package bootstrap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import model.lightchain.Identifier;

/**
 * BootstrapInfo is a data class that contains the information related to a bootstrapped network. It
 * contains the list of docker names, the bootstrap file name, the identifier table, and the list of
 * identifiers.
 * It is meant to be used by the LocalTestNet to create a bootstrapped network.
 */
public class BootstrapInfo {
  /**
   * The docker names is a list that contains the names of the docker containers.
   */
  private final List<String> dockerNames;
  /**
   * The bootstrap file name.
   */
  private final String bootstrapFileName;
  /**
   * The identifier table is a map that contains the node's identifier and the node's address.
   */
  private final HashMap<Identifier, String> identifierTable;

  /**
   * The metrics table is a map that contains the node's identifier and the node's metrics address.
   */
  private final HashMap<Identifier, String> metricsTable;
  /**
   * The list of identifiers.
   */
  private final List<Identifier> identifiers;

  /**
   * Constructor for BootstrapInfo.
   *
   * @param identifiers is the list of identifiers of the nodes in the bootstrapped network.
   * @param dockerNames is the list of docker names of the nodes in the bootstrapped network.
   * @param bootstrapFileName is the name of the bootstrap file.
   * @param identifierTable is the identifier table of the bootstrapped network, which contains the address of each node.
   * @param metricsTable is the metrics table of the bootstrapped network, which contains the metrics address of each node.
   */
  public BootstrapInfo(List<Identifier> identifiers,
                       List<String> dockerNames,
                       String bootstrapFileName,
                       HashMap<Identifier, String> identifierTable,
                       HashMap<Identifier, String> metricsTable) {
    // TODO: implement validate method: size of docker names and identifierTable must be identical, and
    //      the identifiers in the identifierTable must be unique, and the docker names must be unique and docker names should match
    //      the address in the identifier table.
    //      It should throw a RuntimeException if any of the above conditions are not met.
    this.identifiers = new ArrayList<>(identifiers);
    this.dockerNames = new ArrayList<>(dockerNames);
    this.bootstrapFileName = bootstrapFileName;
    this.identifierTable = new HashMap<>(identifierTable);
    this.metricsTable = new HashMap<>(metricsTable);
  }

  /**
   * Returns the list of docker names.
   *
   * @return the list of docker names.
   */
  public List<String> getDockerNames() {
    return new ArrayList<>(dockerNames);
  }

  /**
   * Returns the bootstrap file name.
   *
   * @return the bootstrap file name.
   */
  public String getBootstrapFileName() {
    return bootstrapFileName;
  }

  /**
   * Returns the identifier table.
   *
   * @return the identifier table.
   */
  public HashMap<Identifier, String> getIdentifierTable() {
    return new HashMap<Identifier, String>(identifierTable);
  }

  /**
   * Returns the list of identifiers in the id table.
   *
   * @return the list of identifiers in the id table.
   */
  public Identifier getIdentifier(int i) {
    return this.identifiers.get(i);
  }

  /**
   * Returns the total node count for this bootstrapped system.
   *
   * @return total node count.
   */
  public int size() {
    return this.identifierTable.size();
  }

  /**
   * Returns the metrics table.
   *
   * @return the metrics table.
   */
  public HashMap<Identifier, String> getMetricsTable() {
    return new HashMap<Identifier, String>(metricsTable);
  }
}

