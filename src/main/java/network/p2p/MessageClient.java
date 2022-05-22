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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.Channel;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import model.Entity;
import model.codec.EncodedEntity;
import model.exceptions.CodecException;
import model.lightchain.Identifier;
import modules.codec.JsonEncoder;
import network.p2p.proto.Message;
import network.p2p.proto.MessengerGrpc;

/**
 * Client side of gRPC that is responsible for sending messages from this node.
 */
public class MessageClient {
  private final MessengerGrpc.MessengerStub asyncStub;

  /**
   * Constructor.
   */
  public MessageClient(Channel channel) {
    asyncStub = MessengerGrpc.newStub(channel);
  }

  /**
   * Implements logic to asynchronously deliver message to the target.
   */
  @SuppressFBWarnings(value = "DM_EXIT", justification = "meant to fail VM safely upon error")
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
    } catch (CodecException e) {
      // TODO: replace with fatal level log.
      System.err.println("attempt on delivering an un-encode-ble entity" + e.getMessage());
      System.exit(1);
    }

    // Mark the end of requests
    requestObserver.onCompleted();

    // Receiving happens asynchronously
    if (!finishLatch.await(1, TimeUnit.MINUTES)) {
      System.err.println("deliver can not finish within 1 minutes");
    }
  }
}
