package network.p2p;

import model.Entity;
import model.exceptions.LightChainNetworkingException;
import model.lightchain.Identifier;
import network.Conduit;
import protocol.Engine;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class demo {

  public static void main(String[] args) {

    // set up mock values for testing gRPC communication.

    P2pNetwork n = new P2pNetwork(1);
    n.start();

  }

}
