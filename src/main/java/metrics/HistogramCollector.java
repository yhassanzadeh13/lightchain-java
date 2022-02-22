package metrics;

import io.prometheus.client.Histogram;

/**
 * The HistogramCollector interface is a base interface of counter collector to use for metric collector.
 */
public interface HistogramCollector {
  // TODO: add Java Doc
  Histogram register(String name, String namespace, String subsystem, String helpMessage, double[] buckets)
      throws IllegalArgumentException;
}
