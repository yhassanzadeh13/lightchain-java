package modules;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

import modules.logger.LightchainLogger;
import modules.logger.Logger;

/**
 * ComponentManager is a class that manages the lifecycle of components.
 */
public class ComponentManager implements DeadlineStartable {
  private final Logger logger = LightchainLogger.getLogger(ComponentManager.class.getName());
  private ReentrantLock lock;
  private List<Startable> components;

  private boolean started = false;

  public ComponentManager() {
    this.components = new ArrayList<>();
    this.lock = new ReentrantLock();
  }

  @Override
  public void start(Duration deadline) throws IllegalStateException {
    lock.lock();
    if (started) {
      throw new IllegalStateException("ComponentManager is already started.");
    }
    try {
      CountDownLatch allDone = new CountDownLatch(components.size());
      Thread[] threads = new Thread[components.size()];
      for (int i = 0; i < components.size(); i++) {
        int index = i;
        threads[i] = new Thread(() -> {
          try {
            components.get(index).start();
          } catch (IllegalStateException e) {
            logger.fatal("could not start component", e);
          } finally {
            allDone.countDown();
          }
        });
        threads[i].start();
      }

      try {
        boolean doneOnTime = allDone.await(deadline.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!doneOnTime) {
          throw new IllegalStateException("could not start all components on time");
        }
      } catch (InterruptedException e) {
        throw new IllegalStateException("could not wait for components to start", e);
      }

      started = true;
    } finally {
      lock.unlock();
    }
  }

  public void addComponent(Startable component) {
    components.add(component);
  }
}
