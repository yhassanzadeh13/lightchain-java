package networking;

import java.util.concurrent.ConcurrentHashMap;

import model.Entity;
import model.lightchain.Identifier;
import network.Network;

public class Hub {
  private ConcurrentHashMap<Identifier, Network> networks;
  private ConcurrentHashMap<Identifier, Entity> entities;

  public Hub() {
    this.networks=new ConcurrentHashMap<>();
    this.entities=new ConcurrentHashMap<>();
  }

  public void registerNetwork(Identifier key, Network network) {
    networks.put( key,  network);

  }
  public  StubNetwork getNetwork(Identifier key){

     return (StubNetwork) networks.get(key);

  }

}
