package networking;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import model.Entity;
import model.exceptions.LightChainNetworkingException;
import model.lightchain.Identifier;
import network.Conduit;
import network.Network;
import protocol.Engine;

public class MockEngine implements Engine {
    private final ReentrantReadWriteLock lock;
    private final Set<Identifier> receivedEntityIds;



    public MockEngine() {
        this.receivedEntityIds = new HashSet<>();
        this.lock = new ReentrantReadWriteLock();
    }





    @Override
    public void process(Entity e) throws IllegalArgumentException {
        lock.writeLock();

        receivedEntityIds.add(e.id());

        lock.writeLock();
    }

    public boolean hasReceived(Entity e) {
        lock.readLock();

        Identifier id = e.id();
        boolean ok = this.receivedEntityIds.contains(id);

        lock.readLock();
        return ok;
    }
}