/*
 * Copyright 2015 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package network.p2p;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import model.codec.EncodedEntity;
import modules.codec.JsonEncoder;
import network.p2p.proto.Message;
import network.p2p.proto.MessengerGrpc;
import protocol.Engine;

/**
 * Includes the implementation of server side functionality of gRPC requests.
 */
public class MessageServer {
  private final Server server;
  private final HashMap<String, Engine> engineChannelTable;

  /**
   * Create a MessageServer using ServerBuilder as a base.
   *
   * @param port the TCP port of the target server.
   */
  public MessageServer(int port) {
    server = ServerBuilder.forPort(port)
        .addService(new MessengerImpl())
        .build();

    this.engineChannelTable = new HashMap<>();
  }

  /**
   * Retrieves engine for the given channel.
   *
   * @param channel channel name.
   * @return engine registered on this channel or null.
   */
  public Engine getEngine(String channel) {
    return this.engineChannelTable.get(channel);
  }

  /**
   * Registers an engine on the give channel.
   *
   * @param channel channel for which engine is registered on.
   * @param engine  the engine to be registered on this channel.
   * @throws IllegalStateException if an engine already exists on this channel.
   */
  public void setEngine(String channel, Engine engine) throws IllegalStateException {
    if (this.engineChannelTable.containsKey(channel)) {
      throw new IllegalStateException("channel already exist: " + channel);
    }
    this.engineChannelTable.put(channel, engine);
  }

  /**
   * Returns the port number on which this server is listening.
   *
   * @return the port number on which this server is listening.
   */
  public int getPort() {
    return this.server.getPort();
  }

  /**
   * Start serving requests.
   */
  public void start() throws IOException {
    server.start();
    System.out.println("server started, listening on " + this.getPort());
  }

  /**
   * Halts a running gRPC Server.
   *
   * @throws InterruptedException if the Server process gets interrupted abruptly.
   */
  public void stop() throws InterruptedException {
    server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
  }

  /**
   * Concrete implementation of the gRPC Serverside response methods.
   */
  public class MessengerImpl extends MessengerGrpc.MessengerImplBase {
    /**
     * Function for the gRPC server.
     *
     * @param responseObserver takes in the stream object to receive messages.
     * @return StreamObserver for the Client to facilitate response relaying.
     */
    @Override
    public StreamObserver<Message> deliver(StreamObserver<Empty> responseObserver) {

      return new StreamObserver<Message>() {

        @Override
        public void onNext(Message message) {

          System.out.println("Received Entity");
          System.out.println("OriginID: " + message.getOriginId().toStringUtf8());
          System.out.println("Type: " + message.getType());

          int i = 0;
          for (ByteString s : message.getTargetIdsList()) {
            System.out.println("target " + i++ + ": " + s.toStringUtf8());

            if (engineChannelTable.containsKey(s.toStringUtf8())) {

              try {

                JsonEncoder encoder = new JsonEncoder();
                EncodedEntity e = new EncodedEntity(message.getPayload().toByteArray(), message.getType());

                engineChannelTable.get(s.toStringUtf8()).process(encoder.decode(e));

              } catch (ClassNotFoundException e) {
                e.printStackTrace();
              }

            } else {
              System.out.println("this Network does not have the channel " + s.toStringUtf8());
            }

          }

        }

        @Override
        public void onError(Throwable t) {
          System.err.println("encountered error in deliver: " + t);
        }

        @Override
        public void onCompleted() {
          responseObserver.onNext(com.google.protobuf.Empty.newBuilder().build());
          responseObserver.onCompleted();
        }
      };

    }
  }

}
