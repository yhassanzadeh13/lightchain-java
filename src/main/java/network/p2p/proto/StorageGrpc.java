package network.p2p.proto;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.45.1)",
    comments = "Source: storage.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class StorageGrpc {

  private StorageGrpc() {}

  public static final String SERVICE_NAME = "network.p2p.proto.Storage";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<PutMessage,
      com.google.protobuf.Empty> getPutMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Put",
      requestType = PutMessage.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
  public static io.grpc.MethodDescriptor<PutMessage,
      com.google.protobuf.Empty> getPutMethod() {
    io.grpc.MethodDescriptor<PutMessage, com.google.protobuf.Empty> getPutMethod;
    if ((getPutMethod = StorageGrpc.getPutMethod) == null) {
      synchronized (StorageGrpc.class) {
        if ((getPutMethod = StorageGrpc.getPutMethod) == null) {
          StorageGrpc.getPutMethod = getPutMethod =
              io.grpc.MethodDescriptor.<PutMessage, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Put"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  PutMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new StorageMethodDescriptorSupplier("Put"))
              .build();
        }
      }
    }
    return getPutMethod;
  }

  private static volatile io.grpc.MethodDescriptor<GetRequest,
      GetReply> getGetMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Get",
      requestType = GetRequest.class,
      responseType = GetReply.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<GetRequest,
      GetReply> getGetMethod() {
    io.grpc.MethodDescriptor<GetRequest, GetReply> getGetMethod;
    if ((getGetMethod = StorageGrpc.getGetMethod) == null) {
      synchronized (StorageGrpc.class) {
        if ((getGetMethod = StorageGrpc.getGetMethod) == null) {
          StorageGrpc.getGetMethod = getGetMethod =
              io.grpc.MethodDescriptor.<GetRequest, GetReply>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Get"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  GetRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  GetReply.getDefaultInstance()))
              .setSchemaDescriptor(new StorageMethodDescriptorSupplier("Get"))
              .build();
        }
      }
    }
    return getGetMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static StorageStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<StorageStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<StorageStub>() {
        @Override
        public StorageStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new StorageStub(channel, callOptions);
        }
      };
    return StorageStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static StorageBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<StorageBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<StorageBlockingStub>() {
        @Override
        public StorageBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new StorageBlockingStub(channel, callOptions);
        }
      };
    return StorageBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static StorageFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<StorageFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<StorageFutureStub>() {
        @Override
        public StorageFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new StorageFutureStub(channel, callOptions);
        }
      };
    return StorageFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class StorageImplBase implements io.grpc.BindableService {

    /**
     */
    public io.grpc.stub.StreamObserver<PutMessage> put(
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getPutMethod(), responseObserver);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<GetRequest> get(
        io.grpc.stub.StreamObserver<GetReply> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getGetMethod(), responseObserver);
    }

    @Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getPutMethod(),
            io.grpc.stub.ServerCalls.asyncClientStreamingCall(
              new MethodHandlers<
                PutMessage,
                com.google.protobuf.Empty>(
                  this, METHODID_PUT)))
          .addMethod(
            getGetMethod(),
            io.grpc.stub.ServerCalls.asyncBidiStreamingCall(
              new MethodHandlers<
                GetRequest,
                GetReply>(
                  this, METHODID_GET)))
          .build();
    }
  }

  /**
   */
  public static final class StorageStub extends io.grpc.stub.AbstractAsyncStub<StorageStub> {
    private StorageStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected StorageStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new StorageStub(channel, callOptions);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<PutMessage> put(
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncClientStreamingCall(
          getChannel().newCall(getPutMethod(), getCallOptions()), responseObserver);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<GetRequest> get(
        io.grpc.stub.StreamObserver<GetReply> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncBidiStreamingCall(
          getChannel().newCall(getGetMethod(), getCallOptions()), responseObserver);
    }
  }

  /**
   */
  public static final class StorageBlockingStub extends io.grpc.stub.AbstractBlockingStub<StorageBlockingStub> {
    private StorageBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected StorageBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new StorageBlockingStub(channel, callOptions);
    }
  }

  /**
   */
  public static final class StorageFutureStub extends io.grpc.stub.AbstractFutureStub<StorageFutureStub> {
    private StorageFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected StorageFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new StorageFutureStub(channel, callOptions);
    }
  }

  private static final int METHODID_PUT = 0;
  private static final int METHODID_GET = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final StorageImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(StorageImplBase serviceImpl, int methodId) {
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
        case METHODID_PUT:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.put(
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
        case METHODID_GET:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.get(
              (io.grpc.stub.StreamObserver<GetReply>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class StorageBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    StorageBaseDescriptorSupplier() {}

    @Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return StorageOuterClass.getDescriptor();
    }

    @Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Storage");
    }
  }

  private static final class StorageFileDescriptorSupplier
      extends StorageBaseDescriptorSupplier {
    StorageFileDescriptorSupplier() {}
  }

  private static final class StorageMethodDescriptorSupplier
      extends StorageBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    StorageMethodDescriptorSupplier(String methodName) {
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
      synchronized (StorageGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new StorageFileDescriptorSupplier())
              .addMethod(getPutMethod())
              .addMethod(getGetMethod())
              .build();
        }
      }
    }
    return result;
  }
}
