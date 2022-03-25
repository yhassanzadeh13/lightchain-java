package networking;

import java.util.concurrent.ConcurrentHashMap;

import model.Entity;
import model.lightchain.Identifier;
import network.Network;

/**
 * Models the core communication part of the networking layer that allows stub network instances to talk to each other.
 */
public class Hub {
    private final ConcurrentHashMap<Identifier, Network> networks;
    private final ConcurrentHashMap<Identifier, Entity> entities;

    public Hub() {
        this.networks = new ConcurrentHashMap<>();
        this.entities = new ConcurrentHashMap<>();
    }

    public void registerNetwork(Identifier key, Network network) {
        networks.put(key, network);

    }

    public void transferEntity(Entity entity, Identifier identifier, String channel) {
        StubNetwork net = this.getNetwork(identifier);
        net.receiveUnicast(entity, channel);

    }

    private StubNetwork getNetwork(Identifier key) {
        return (StubNetwork) networks.get(key);
    }

}