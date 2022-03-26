package metrics;

import io.prometheus.client.Gauge;

/**
 * The GaugeCollector interface is a base interface of counter collector to use for metric collector.
 */
public interface GaugeCollector {
  /**
   * Registers a gauge collector.
   *
   * @param name        name of gauge metric.
   * @param namespace   namespace of gauge metric, normally refers to a distinct class of LightChain, e.g., network.
   * @param subsystem   either the same as namespace for monolith classes, or the subclass for which we collect metrics,
   *                    e.g., network.latency generator within middleware.
   * @param helpMessage a hint message describing what this metric represents.
   * @return the registered gauge metric.
   * @throws IllegalArgumentException when a different metric type with the
   *                                  same name has already been registered.
   */
  Gauge register(String name, String namespace, String subsystem, String helpMessage) throws IllegalArgumentException;

}
