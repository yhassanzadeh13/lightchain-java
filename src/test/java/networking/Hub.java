package networking;

import java.util.concurrent.ConcurrentHashMap;

import model.Entity;
import model.lightchain.Identifier;
import network.Network;

/**
 * Models the core communication part of the networking layer that allows stub network instances to talk to each other.
 */
public class Hub {
  private ConcurrentHashMap<Identifier, Network> networks;
  private ConcurrentHashMap<Identifier, Entity> entities;
}
