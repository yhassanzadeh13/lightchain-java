package metrics;

import io.prometheus.client.Counter;

/**
 * The CounterCollector interface is a base interface of counter collector to use for metric collector.
 */
public interface CounterCollector {

  /**
   * Registers a counter collector.
   *
   * @param name        name of counter metric.
   * @param namespace   namespace of counter metric, normally refers to a distinct class of LightChain, e.g., network.
   * @param subsystem   either the same as namespace for monolith classes, or the subclass for which we collect metrics,
   *                    e.g., network.latency generator within middleware.
   * @param helpMessage a hint message describing what this metric represents.
   * @return the registered counter metric.
   * @throws IllegalArgumentException when a different metric type (e.g., histogram) with the
   *                                  same name has already been registered.
   */
  Counter register(String name, String namespace, String subsystem, String helpMessage) throws IllegalArgumentException;

}
