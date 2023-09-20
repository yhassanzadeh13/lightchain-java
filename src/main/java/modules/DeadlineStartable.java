package modules;

import java.time.Duration;

/**
 * DeadlineStartable is an interface that defines the start method with a deadline.
 */
public interface DeadlineStartable {
  void start(Duration deadline) throws IllegalStateException;
}
