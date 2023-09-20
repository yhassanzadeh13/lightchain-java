package metrics.integration;

/**
 * ContainerAlreadyExistsException is thrown when a container with the same name already exists.
 */
public class ContainerAlreadyExistsException extends Exception {
  public ContainerAlreadyExistsException(String message) {
    super(message);
  }
}
