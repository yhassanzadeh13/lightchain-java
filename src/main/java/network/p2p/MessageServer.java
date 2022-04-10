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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import model.codec.EncodedEntity;
import modules.codec.JsonEncoder;
import protocol.Engine;

/**
 * Includes the implementation of server side functionality of gRPC requests.
 */
public class MessageServer {

  private static final Logger logger = Logger.getLogger(MessageServer.class.getName());
  private final int port;
  private final Server server;
  HashMap<String, Engine> engineChannelTable;

  /**
   * Create a MessageServer using ServerBuilder as a base.
   *
   * @param port the TCP port of the target server.
   */
  public MessageServer(int port) {
    this.port = port;
    server = ServerBuilder.forPort(port)
            .addService(new MessengerImpl())
            .build();

    this.engineChannelTable = new HashMap<String, Engine>();
  }

  /**
   * Start serving requests.
   */
  public void start() throws IOException {
    server.start();
    logger.info("Server started, listening on " + port);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        try {
          MessageServer.this.stop();
        } catch (InterruptedException e) {
          e.printStackTrace(System.err);
        }
        System.err.println("*** server shut down");
      }
    });
  }

  /**
   * Halts a running gRPC Server.
   *
   * @throws InterruptedException if the Server process gets interrupted abruptly.
   */
  public void stop() throws InterruptedException {
    if (server != null) {
      server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
    }
  }

  /**
   * Await termination on the main thread since the grpc library uses daemon threads.
   *
   * @throws InterruptedException if the Server process gets interrupted abruptly.
   */
  public void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  /**
   * Concrete implementation of the gRPC Serverside response methods.
   */
  public class MessengerImpl extends MessengerGrpc.MessengerImplBase {
    private final Logger logger = Logger.getLogger(MessengerImpl.class.getName());

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
            System.out.println("Target " + i++ + ": " + s.toStringUtf8());

            if (engineChannelTable.containsKey(s.toStringUtf8())) {

              try {

                JsonEncoder encoder = new JsonEncoder();
                EncodedEntity e = new EncodedEntity(message.getPayload().toByteArray(), message.getType());

                engineChannelTable.get(s.toStringUtf8()).process(encoder.decode(e));

              } catch (ClassNotFoundException e) {
                e.printStackTrace();
              }

            } else {
              System.out.println("This Network does not have the channel " + s.toStringUtf8());
            }

          }

        }

        @Override
        public void onError(Throwable t) {
          logger.log(Level.WARNING, "Encountered error in deliver", t);
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
