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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.grpc.Channel;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import model.Entity;
import model.codec.EncodedEntity;
import model.lightchain.Identifier;
import modules.codec.JsonEncoder;
import network.p2p.proto.Message;
import network.p2p.proto.MessengerGrpc;
import network.p2p.proto.GetReply;
import network.p2p.proto.GetRequest;
import network.p2p.proto.PutMessage;
import network.p2p.proto.StorageGrpc;

/**
 * Client side of gRPC that is responsible for sending messages from this node.
 */
public class MessageClient {
  private final MessengerGrpc.MessengerStub asyncStub;
  private final StorageGrpc.StorageStub storageAsyncStub;


  /**
   * Constructor.
   */
  public MessageClient(Channel channel) {

    asyncStub = MessengerGrpc.newStub(channel);
    storageAsyncStub = StorageGrpc.newStub(channel);

  }

  /**
   * Implements logic to asynchronously deliver message to the target.
   */
  public void deliver(Entity entity, Identifier target, String channel) throws InterruptedException {
    final CountDownLatch finishLatch = new CountDownLatch(1);
    StreamObserver<Empty> responseObserver = new StreamObserver<Empty>() {
      @Override
      public void onNext(Empty value) {

      }

      @Override
      public void onError(Throwable t) {
        System.err.println("deliver failed: " + Status.fromThrowable(t));
        finishLatch.countDown();
      }

      @Override
      public void onCompleted() {
        finishLatch.countDown();
      }
    };

    StreamObserver<Message> requestObserver = asyncStub.deliver(responseObserver);
    try {
      JsonEncoder encoder = new JsonEncoder();

      EncodedEntity encodedEntity = encoder.encode(entity);
      Message message = Message.newBuilder()
              .setChannel(channel)
              .setPayload(ByteString.copyFrom(encodedEntity.getBytes()))
              .setType(encodedEntity.getType())
              .addTargetIds(ByteString.copyFrom(target.getBytes()))
              .build();
      requestObserver.onNext(message);

      if (finishLatch.getCount() == 0) {
        // RPC completed or errored before we finished sending.
        // Sending further requests won't error, but they will just be thrown away.
        return;
      }

    } catch (RuntimeException e) {
      // Cancel RPC
      requestObserver.onError(e);
      throw e;
    }

    // Mark the end of requests
    requestObserver.onCompleted();

    // Receiving happens asynchronously
    if (!finishLatch.await(1, TimeUnit.MINUTES)) {
      System.err.println("deliver can not finish within 1 minutes");
    }
  }

  /**
   * Implements logic to asynchronously put entity to the target.
   */
  public void put(Entity entity, String channel) throws InterruptedException {
    final CountDownLatch finishLatch = new CountDownLatch(1);
    StreamObserver<Empty> responseObserver = new StreamObserver<Empty>() {
      @Override
      public void onNext(Empty value) {

      }

      @Override
      public void onError(Throwable t) {
        System.err.println("put failed: " + Status.fromThrowable(t));
        finishLatch.countDown();
      }

      @Override
      public void onCompleted() {
        finishLatch.countDown();
      }
    };

    StreamObserver<PutMessage> requestObserver = storageAsyncStub.put(responseObserver);

    try {
      JsonEncoder encoder = new JsonEncoder();

      EncodedEntity encodedEntity = encoder.encode(entity);
      PutMessage putMessage = PutMessage.newBuilder()
              .setChannel(channel)
              .setPayload(ByteString.copyFrom(encodedEntity.getBytes()))
              .setType(encodedEntity.getType())
              .build();
      requestObserver.onNext(putMessage);

      if (finishLatch.getCount() == 0) {
        // RPC completed or errored before we finished sending.
        // Sending further requests won't error, but they will just be thrown away.
        return;
      }

    } catch (RuntimeException e) {
      // Cancel RPC
      requestObserver.onError(e);
      throw e;
    }

    // Mark the end of requests
    requestObserver.onCompleted();

    // Receiving happens asynchronously
    if (!finishLatch.await(1, TimeUnit.MINUTES)) {
      System.err.println("put can not finish within 1 minutes");
    }
  }

  public Entity get(Identifier identifier) throws InterruptedException {

    final CountDownLatch finishLatch = new CountDownLatch(1);
    final Entity[] entity = {null};
    StreamObserver<GetRequest> requestObserver =
            storageAsyncStub.get(new StreamObserver<GetReply>() {

              @Override
              public void onNext(GetReply response) {

                JsonEncoder encoder = new JsonEncoder();
                EncodedEntity e = new EncodedEntity(response.getPayload().toByteArray(), response.getType());
                try {
                  // puts the incoming entity onto the distributedStorageComponent
                  entity[0] = encoder.decode(e);
                } catch (ClassNotFoundException ex) {
                  // TODO: replace with fatal log
                  System.err.println("could not decode incoming GetReply response");
                  ex.printStackTrace();
                  System.exit(1);
                }

              }

              @Override
              public void onError(Throwable t) {
                finishLatch.countDown();
              }

              @Override
              public void onCompleted() {
                finishLatch.countDown();
              }

            });

    try {

      GetRequest request = GetRequest
              .newBuilder()
              .setIdentifier(ByteString.copyFrom(identifier.getBytes()))
              .build();

      requestObserver.onNext(request);

    } catch (RuntimeException e) {
      // Cancel RPC
      requestObserver.onError(e);
      throw e;
    }
    // Mark the end of requests
    requestObserver.onCompleted();

    // Receiving happens asynchronously
    finishLatch.await(1, TimeUnit.MINUTES);

    return entity[0];

  }


}
