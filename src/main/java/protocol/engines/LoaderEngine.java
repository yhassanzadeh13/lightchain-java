package protocol.engines;

import java.util.ArrayList;

import model.Entity;
import model.crypto.PrivateKey;
import protocol.Engine;
import state.State;

/**
 * Loader engine is responsible for sending loads of transactions in the network.
 */
public class LoaderEngine implements Engine {
  public LoaderEngine(State state, ArrayList<PrivateKey> keys) {

  }

  @Override
  public void process(Entity e) throws IllegalArgumentException {

  }

  /**
   * Creates a load of transactions of given size.
   *
   * @param size total number of transactions.
   * @throws IllegalStateException any illegal state face.
   */
  public void createLoad(int size) throws IllegalStateException {

  }
}
