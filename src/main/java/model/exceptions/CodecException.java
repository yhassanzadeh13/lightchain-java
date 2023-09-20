package model.exceptions;

/**
 * Represents a runtime exception happens on encoding or decoding process.
 */
public class CodecException extends Exception {
  public CodecException(String message, Throwable cause) {
    super(message, cause);
  }
}
