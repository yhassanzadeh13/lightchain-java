// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: storage.proto

package network.p2p.proto;

public interface PutMessageOrBuilder extends
    // @@protoc_insertion_point(interface_extends:network.p2p.proto.PutMessage)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>bytes OriginId = 1;</code>
   * @return The originId.
   */
  com.google.protobuf.ByteString getOriginId();

  /**
   * <code>string Channel = 2;</code>
   * @return The channel.
   */
  java.lang.String getChannel();
  /**
   * <code>string Channel = 2;</code>
   * @return The bytes for channel.
   */
  com.google.protobuf.ByteString
      getChannelBytes();

  /**
   * <code>bytes Payload = 3;</code>
   * @return The payload.
   */
  com.google.protobuf.ByteString getPayload();

  /**
   * <code>string Type = 4;</code>
   * @return The type.
   */
  java.lang.String getType();
  /**
   * <code>string Type = 4;</code>
   * @return The bytes for type.
   */
  com.google.protobuf.ByteString
      getTypeBytes();
}
