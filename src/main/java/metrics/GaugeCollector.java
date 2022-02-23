package metrics;

import io.prometheus.client.Gauge;

/**
 * The GaugeCollector interface is a base interface of counter collector to use for metric collector.
 */
public interface GaugeCollector {
  // TODO: add java doc
  Gauge register(String name, String namespace, String subsystem, String helpMessage) throws IllegalArgumentException;
}
