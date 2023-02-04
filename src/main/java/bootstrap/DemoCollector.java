package bootstrap;

import io.prometheus.client.Counter;
import metrics.Collector;
import metrics.collectors.LightChainCollector;

/**
 * Metrics Collector for the BroadcastEngine class. This class is responsible for registering and
 * incrementing the metrics for the BroadcastEngine class. We use this for sake of demonstration.
 */
public class DemoCollector {
  private final Counter helloMessageReceiveCount;

  /**
   * Constructor for DemoCollector.
   */
  public DemoCollector() {
    Collector collector = new LightChainCollector();
    this.helloMessageReceiveCount = collector.counter().register("hello_message_receive_count",
        "broadcast_engine", "demo", "total hello messages received");
  }

  /**
   * Increments the helloMessageReceiveCount metric by 1.
   */
  public void onHelloMessageReceived() {
    this.helloMessageReceiveCount.inc(1);
  }
}
