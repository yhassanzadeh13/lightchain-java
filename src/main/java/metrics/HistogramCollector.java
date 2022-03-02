package metrics;

import io.prometheus.client.Histogram;

/**
 * The HistogramCollector interface is a base interface of counter collector to use for metric collector.
 */
public interface HistogramCollector {
  /**
   * Registers a histogram collector.
   *
   * @param name        name of histogram metric.
   * @param namespace   namespace of histogram metric, normally refers to a distinct class of LightChain, e.g., network.
   * @param subsystem   either the same as namespace for monolith classes, or the subclass for which we collect metrics,
   *                    e.g., network.latency generator within middleware.
   * @param helpMessage a hint message describing what this metric represents.
   * @return the registered histogram metric.
   * @throws IllegalArgumentException when a different metric type with the
   *                                  same name has already been registered.
   */
  Histogram register(String name, String namespace, String subsystem, String helpMessage, double[] buckets)
          throws IllegalArgumentException;
}
