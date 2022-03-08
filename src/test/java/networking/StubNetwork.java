package networking;

import java.util.concurrent.ConcurrentHashMap;

import model.Entity;
import model.exceptions.LightChainDistributedStorageException;
import model.exceptions.LightChainNetworkingException;
import model.lightchain.Identifier;
import network.Conduit;
import network.Network;
import protocol.Engine;

public class StubNetwork implements Network {
  private ConcurrentHashMap<String, Engine> engines;
  private Hub hub;

  public StubNetwork(Hub hub){
    this.engines = new ConcurrentHashMap<>();
    this.hub = hub;
  }

  @Override
  public Conduit register(Engine e, String channel) throws IllegalStateException {
      Conduit conduit = new Conduit() {
        @Override
        public void unicast(Entity e, Identifier target) throws LightChainNetworkingException {
          System.out.println();
        }

        @Override
        public void put(Entity e) throws LightChainDistributedStorageException {

        }

        @Override
        public Entity get(Identifier identifier) throws LightChainDistributedStorageException {
          return null;
        }
      };
    return conduit;
  }
}
