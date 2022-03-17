package networking;

import model.Entity;
import model.exceptions.LightChainNetworkingException;
import model.lightchain.Identifier;
import network.Conduit;
import network.Network;
import protocol.Engine;
import unittest.fixtures.IdentifierFixture;

import java.util.concurrent.ConcurrentHashMap;

public class StubNetwork implements Network {
    private final ConcurrentHashMap<String, Engine> engines;

    private final Hub hub;
    private final Identifier identifier;


    public StubNetwork(Hub hub) {
        this.engines = new ConcurrentHashMap<>();

        this.hub = hub;
        this.identifier = IdentifierFixture.newIdentifier();
        this.hub.registerNetwork(identifier, this);
    }

    public Identifier id() {
        return this.identifier;
    }


    public void receiveUnicast(Entity entity, String channel) {
        Engine engine = getEngine(channel);
        try {
            engine.process(entity);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("could not process the entity" + e);
        }
    }

    @Override
    public Conduit register(Engine en, String channel) throws IllegalStateException {
        //
        Conduit conduit = new MockConduit(channel, hub);
        try {
            if (engines.containsKey(channel)) {
                throw new IllegalStateException();
            }
            engines.put(channel, en);


        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("could not register the engine" + ex);
        }

        return conduit;
    }

    public Engine getEngine(String ch) {
        return engines.get(ch);
    }

}