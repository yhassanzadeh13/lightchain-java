package modules;

import java.time.Duration;

public interface DeadlineStartable {
  void start(Duration deadline) throws IllegalStateException;
}
