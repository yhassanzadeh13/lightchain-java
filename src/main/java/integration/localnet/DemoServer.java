package integration.localnet;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import metrics.collectors.LightChainCollector;
import metrics.collectors.MetricServer;

/**
 * Demonstrative class for setting up an HTTP Endpoint Server for Prometheus metric exposure.
 * Note that DemoServer is not meant to be run independently, rather it is containerized and
 * executed in docker runtime by the Cmd class.
 */
public class DemoServer {
  /**
   * main function.
   *
   * @param args standard Java args
   */
  public static void main(String[] args) {
    Random r = new Random();
    MetricServer server = new MetricServer();
    LightChainCollector collector = new LightChainCollector();

    Counter demoServerQueryCount = null;
    Gauge demoServerQueryGauge = null;

    // registers metrics.
    try {
      demoServerQueryCount = collector.counter().register("demo_server_query_count",
              "localnet", "demo", "Demo server query count");

      demoServerQueryGauge = collector.gauge().register("demo_server_query_gauge",
              "localnet", "demo", "Demo server query gauge");
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("could not initialize the metrics with the provided arguments", ex);
    }

    try {
      server.start();
    } catch (IllegalStateException e) {
      throw new IllegalStateException("could not start the Metric Server", e);
    }

    while (true) {
      try {
        TimeUnit.SECONDS.sleep(1);
        demoServerQueryCount.inc(1);
        demoServerQueryGauge.set(r.nextInt(100));
      } catch (InterruptedException e) {
        System.err.println("Thread sleep issue, breaking the loop");
        break;
      }
    }

    try {
      server.terminate();
    } catch (Exception e) {
      throw new IllegalStateException("could not terminate the Metric Server", e);
    }
  }
}
