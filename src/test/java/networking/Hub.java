package networking;

import java.util.concurrent.ConcurrentHashMap;

import model.Entity;
import model.lightchain.Identifier;
import network.Network;

public class Hub {
  //abdullah
  private ConcurrentHashMap<Identifier, Network> networks;
  private ConcurrentHashMap<Identifier, Entity> entities;
}
