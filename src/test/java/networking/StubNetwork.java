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
        this.identifier = IdentifierFixture.NewIdentifier();
        this.hub.registerNetwork(identifier, this);
    }

    public Identifier id() {
        return this.identifier;
    }


    public  void receiveUnicast(Entity entity, String channel){
        Engine engine =getEngine(channel);
        engine.process(entity);
    }
    @Override
    public Conduit register(Engine en, String channel) throws IllegalStateException {
        //
        Conduit conduit = new MockConduit(channel,hub);
        try {
            if (engines.containsKey(channel)) {
                throw new IllegalStateException();
            }
            engines.put(channel, en);



        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return conduit;
    }

    private Engine getEngine(String ch) {
        return engines.get(ch);
    }

}