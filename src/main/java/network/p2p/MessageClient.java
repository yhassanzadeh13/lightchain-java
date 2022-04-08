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

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import model.Entity;
import model.lightchain.Identifier;
import protocol.Engine;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessageClient {

  private static final Logger logger = Logger.getLogger(MessageClient.class.getName());
  private final MessengerGrpc.MessengerBlockingStub blockingStub;
  private final MessengerGrpc.MessengerStub asyncStub;

  /**
   * Construct client for accessing MessageServer using the existing channel.
   */
  public MessageClient(Channel channel) {
    blockingStub = MessengerGrpc.newBlockingStub(channel);
    asyncStub = MessengerGrpc.newStub(channel);
  }

  /**
   * Async client-streaming.
   */
  public void deliver(Entity entity, Identifier target, Engine sourceEngine) throws InterruptedException {
    info("*** deliver");
    final CountDownLatch finishLatch = new CountDownLatch(1);
    StreamObserver<Empty> responseObserver = new StreamObserver<Empty>() {

      @Override
      public void onNext(Empty value) {
        info("Sent message");
      }

      @Override
      public void onError(Throwable t) {
        warning("deliver Failed: {0}", Status.fromThrowable(t));
        finishLatch.countDown();
      }

      @Override
      public void onCompleted() {
        info("Finished deliver");
        finishLatch.countDown();
      }
    };

    StreamObserver<Message> requestObserver = asyncStub.deliver(responseObserver);
    try {

        Message message = Message.newBuilder()
                .setOriginId(ByteString.copyFromUtf8("" + sourceEngine.toString()))
                .setPayload(ByteString.copyFromUtf8("Entity No: " + entity.id()))
                .setType(entity.type())
                .addTargetIds(ByteString.copyFromUtf8(target.toString()))
                .build();
        requestObserver.onNext(message);

        // Sleep for a bit before sending the next one. Will be useful for sequential messages.
        Thread.sleep(1000);

      Message message2 = Message.newBuilder()
              .setOriginId(ByteString.copyFromUtf8("" + sourceEngine))
              .setPayload(ByteString.copyFromUtf8("Entity No: " + entity.id()))
              .setType(entity.type())
              .addTargetIds(ByteString.copyFromUtf8(target.toString()))
              .build();
      requestObserver.onNext(message2);

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
      warning("deliver can not finish within 1 minutes");
    }
  }

  private void info(String msg, Object... params) {
    logger.log(Level.INFO, msg, params);
  }

  private void warning(String msg, Object... params) {
    logger.log(Level.WARNING, msg, params);
  }

}
