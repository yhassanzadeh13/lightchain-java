package networking;

import java.util.concurrent.ConcurrentHashMap;

import model.Entity;
import model.exceptions.LightChainDistributedStorageException;
import model.exceptions.LightChainNetworkingException;
import model.lightchain.Identifier;
import network.Conduit;
import network.Network;
import protocol.Engine;
import unittest.fixtures.IdentifierFixture;

public class StubNetwork implements Network {
    private ConcurrentHashMap<String, Engine> engines;
    private ConcurrentHashMap<String, Conduit> conduits;
    private Hub hub;
    private Identifier identifier;



    public StubNetwork(Hub hub) {
        this.engines = new ConcurrentHashMap<>();
        this.conduits= new ConcurrentHashMap<>();
        this.hub = hub;
        this.identifier = IdentifierFixture.NewIdentifier();
        this.hub.registerNetwork(identifier, this);
    }

    public Identifier id() {
        return this.identifier;
    }
    public void sendUnicast(String ch, StubNetwork stubNetworkR, Entity entity) throws LightChainNetworkingException {
        Conduit conduit = conduits.get(ch);
        conduit.unicast(entity,stubNetworkR.id());

    }

    @Override
    public Conduit register(Engine en, String channel) throws IllegalStateException {
        Conduit conduit = new Conduit() {
            @Override
            public void unicast(Entity e, Identifier target) throws LightChainNetworkingException {
                StubNetwork net = hub.getNetwork(target);
                net.deliverEntity(channel, e);

            }

            @Override
            public void put(Entity e) throws LightChainDistributedStorageException {

            }

            @Override
            public Entity get(Identifier identifier) throws LightChainDistributedStorageException {
                return null;
            }
        };
        try {
            if (engines.containsKey(channel)) {
                throw new IllegalStateException();
            }

            engines.put(channel, en);

            conduits.put(channel, conduit);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return conduit;
    }

    public void deliverEntity(String ch, Entity en) {
        System.out.println(ch);

        Engine engine = engines.get(ch);
        engine.process(en);

    }
}
