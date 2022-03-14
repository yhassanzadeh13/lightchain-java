package networking;

import model.Entity;
import model.exceptions.LightChainDistributedStorageException;
import model.exceptions.LightChainNetworkingException;
import model.lightchain.Identifier;
import network.Conduit;
import network.Network;
import protocol.Engine;

public class MockConduit implements Conduit {
    private String channel;
    private Engine engine;
    private Hub hub;

    public MockConduit(String channel, Engine engine, Hub hub) {
        this.channel = channel;
        this.engine = engine;
        this.hub = hub;
    }

    /**
     * Sends the Entity through the Network to the remote target.
     *
     * @param e      the Entity to be sent over the network.
     * @param target Identifier of the receiver.
     * @throws LightChainNetworkingException any unhappy path taken on sending the Entity.
     */
    @Override
    public void unicast(Entity e, Identifier target) throws LightChainNetworkingException {
        StubNetwork net = hub.getNetwork(target);
        deliverEntity(net,channel , e);
    }

    /**
     * Stores given Entity on the underlying Distributed Hash Table (DHT) of nodes.
     *
     * @param e the Entity to be stored over the network.
     * @throws LightChainDistributedStorageException any unhappy path taken on storing the Entity.
     */
    @Override
    public void put(Entity e) throws LightChainDistributedStorageException {

    }

    /**
     * Retrieves the entity corresponding to the given identifier form the underlying Distributed Hash Table
     * (DHT) of nodes.
     *
     * @param identifier identifier of the entity to be retrieved.
     * @return the retrieved entity or null if it does not exist.
     * @throws LightChainDistributedStorageException any unhappy path taken on retrieving the Entity.
     */
    @Override
    public Entity get(Identifier identifier) throws LightChainDistributedStorageException {
        return null;
    }
    public void deliverEntity(StubNetwork network,String channel, Entity en) {
        Engine eng =network.getEngine(channel);
        eng.process(en);
    }
}