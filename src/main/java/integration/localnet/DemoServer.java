package integration.localnet;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import metrics.collectors.LightChainCollector;

/** Demonstrative Class for setting up an HTTP Endpoint Server for Prometheus metric exposure.
 */
public class DemoServer {

  static HTTPServer server;
  static LightChainCollector collector;
  static Counter demoServerQueryCount;
  static Gauge demoServerQueryGauge;
  private static final int SERVER_PORT = 8081;

  /** main function.
   *
   * @param args standart Java args
   */
  public static void main(String[] args) {

    Random r = new Random();

    try {

      collector = new LightChainCollector();

      demoServerQueryCount = collector.counter().register("demo_server_query_count",
              "localnet", "demo", "Demo server query count");

      demoServerQueryGauge = collector.gauge().register("demo_server_query_gauge",
              "localnet", "demo", "Demo server query gauge");

    } catch (IllegalArgumentException ex) {
      System.err.println("Could not initialize the metrics with the provided arguments" + ex);
      System.exit(1);
    }

    try {
      server = new HTTPServer(SERVER_PORT);
    } catch (IOException e) {
      System.err.println("Could not start the Metric Server:\t" + e);
      System.exit(1);
    }

    while (true) {
      try {
        TimeUnit.SECONDS.sleep(5);
        demoServerQueryCount.inc(1);
        demoServerQueryGauge.set(r.nextInt(100));
      } catch (InterruptedException e) {
        System.err.println("could not start metrics server:\t" + e);
        break;
      }
    }

    try {
      server.close();
    } catch (Exception e) {
      System.err.println("Could not stop the Metric Server:\t" + e);
      System.exit(1);
    }

  }

}
