package metrics.collectors;

import io.prometheus.client.Histogram;
import metrics.HistogramCollector;

/**
 * LightChain Histogram is the LightChain implementation of the Histogram Collector interface.
 */
public class LightChainHistogram implements HistogramCollector {
  /**
   * Registers a histogram collector.
   *
   * @param name        name of histogram metric.
   * @param namespace   namespace of histogram metric, normally refers to a distinct class of LightChain, e.g., network.
   * @param subsystem   either the same as namespace for monolith classes, or the subclass for which we collect metrics,
   *                    e.g., network.latency generator within middleware.
   * @param helpMessage a hint message describing what this metric represents.
   * @param buckets     buckets of histogram
   * @return the registered histogram metric
   * @throws IllegalArgumentException when a different metric type with the
   *                                  same name has already been registered.
   */
  @Override
  public Histogram register(String name, String namespace, String subsystem,
                            String helpMessage, double[] buckets) throws IllegalArgumentException {
    return Histogram
        .build()
        .namespace(namespace)
        .subsystem(subsystem)
        .name(name)
        .help(helpMessage)
        .register();
  }

}
