package model.exceptions;

/**
 * Represents a runtime exception happens on the Networking layer of LightChain.
 */
public class LightChainNetworkingException extends Exception{
  public LightChainNetworkingException(String message, Throwable cause) {
    super(message, cause);
  }
}
