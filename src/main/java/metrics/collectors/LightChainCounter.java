package metrics.collectors;

import io.prometheus.client.Counter;
import metrics.CounterCollector;

/**
 * LightChain Counter is the LightChain implementation of the Counter Collector interface.
 */
public class LightChainCounter implements CounterCollector {
  /**
   * Registers a counter collector.
   *
   * @param name        name of counter metric.
   * @param namespace   namespace of counter metric, normally refers to a distinct class of LightChain, e.g., network.
   * @param subsystem   either the same as namespace for monolith classes,
   *                    or the subclass for which we collect metrics,
   *                    e.g., network.latency generator within middleware.
   * @param helpMessage a hint message describing what this metric represents.
   * @return the registered counter metric
   * @throws IllegalArgumentException when a different metric type with the
   *                                  same name has already been registered.
   */
  @Override
  public Counter register(String name, String namespace, String subsystem, String helpMessage)
      throws IllegalArgumentException {
    return Counter
        .build()
        .namespace(namespace)
        .subsystem(subsystem)
        .name(name)
        .help(helpMessage)
        .register();
  }
}
