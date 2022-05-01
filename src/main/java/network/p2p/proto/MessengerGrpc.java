package network.p2p.proto;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.45.1)",
    comments = "Source: message.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class MessengerGrpc {

  private MessengerGrpc() {}

  public static final String SERVICE_NAME = "network.p2p.proto.Messenger";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<Message,
      com.google.protobuf.Empty> getDeliverMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Deliver",
      requestType = Message.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
  public static io.grpc.MethodDescriptor<Message,
      com.google.protobuf.Empty> getDeliverMethod() {
    io.grpc.MethodDescriptor<Message, com.google.protobuf.Empty> getDeliverMethod;
    if ((getDeliverMethod = MessengerGrpc.getDeliverMethod) == null) {
      synchronized (MessengerGrpc.class) {
        if ((getDeliverMethod = MessengerGrpc.getDeliverMethod) == null) {
          MessengerGrpc.getDeliverMethod = getDeliverMethod =
              io.grpc.MethodDescriptor.<Message, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Deliver"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Message.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new MessengerMethodDescriptorSupplier("Deliver"))
              .build();
        }
      }
    }
    return getDeliverMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static MessengerStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MessengerStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MessengerStub>() {
        @Override
        public MessengerStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MessengerStub(channel, callOptions);
        }
      };
    return MessengerStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static MessengerBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MessengerBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MessengerBlockingStub>() {
        @Override
        public MessengerBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MessengerBlockingStub(channel, callOptions);
        }
      };
    return MessengerBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static MessengerFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MessengerFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MessengerFutureStub>() {
        @Override
        public MessengerFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MessengerFutureStub(channel, callOptions);
        }
      };
    return MessengerFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class MessengerImplBase implements io.grpc.BindableService {

    /**
     */
    public io.grpc.stub.StreamObserver<Message> deliver(
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getDeliverMethod(), responseObserver);
    }

    @Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getDeliverMethod(),
            io.grpc.stub.ServerCalls.asyncClientStreamingCall(
              new MethodHandlers<
                Message,
                com.google.protobuf.Empty>(
                  this, METHODID_DELIVER)))
          .build();
    }
  }

  /**
   */
  public static final class MessengerStub extends io.grpc.stub.AbstractAsyncStub<MessengerStub> {
    private MessengerStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected MessengerStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MessengerStub(channel, callOptions);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<Message> deliver(
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncClientStreamingCall(
          getChannel().newCall(getDeliverMethod(), getCallOptions()), responseObserver);
    }
  }

  /**
   */
  public static final class MessengerBlockingStub extends io.grpc.stub.AbstractBlockingStub<MessengerBlockingStub> {
    private MessengerBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected MessengerBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MessengerBlockingStub(channel, callOptions);
    }
  }

  /**
   */
  public static final class MessengerFutureStub extends io.grpc.stub.AbstractFutureStub<MessengerFutureStub> {
    private MessengerFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected MessengerFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MessengerFutureStub(channel, callOptions);
    }
  }

  private static final int METHODID_DELIVER = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final MessengerImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(MessengerImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }

    @Override
    @SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_DELIVER:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.deliver(
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class MessengerBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    MessengerBaseDescriptorSupplier() {}

    @Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return MessageOuterClass.getDescriptor();
    }

    @Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Messenger");
    }
  }

  private static final class MessengerFileDescriptorSupplier
      extends MessengerBaseDescriptorSupplier {
    MessengerFileDescriptorSupplier() {}
  }

  private static final class MessengerMethodDescriptorSupplier
      extends MessengerBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    MessengerMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (MessengerGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new MessengerFileDescriptorSupplier())
              .addMethod(getDeliverMethod())
              .build();
        }
      }
    }
    return result;
  }
}
