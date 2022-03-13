package integration.localnet;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;

/** Demonstrative Class for setting up an HTTP Endpoint Server for Prometheus metric exposure.
 */
public class DemoServer {

  static final Counter demoServerQueryCount = Counter.build()
          .name("localnet_demo_demo_server_query_count").help("Demo server query count").register();
  static final Gauge demoServerQueryGauge = Gauge.build()
          .name("localnet_demo_demo_server_query_gauge").help("Demo server query gauge").register();
  static HTTPServer server;

  /** main function.
   *
   * @param args standart Java args
   */
  public static void main(String[] args) {

    Random r = new Random();

    try {
      server = new HTTPServer(8080);
    } catch (IOException e) {
      throw new IllegalStateException("could not start metrics server:\t" + e);
    }

    try {
      while (true) {
        TimeUnit.SECONDS.sleep(5);
        demoServerQueryCount.inc(1);
        demoServerQueryGauge.set(r.nextInt(100));
      }
    } catch (InterruptedException e) {
      throw new IllegalStateException("could not start metrics server:\t" + e);
    }

  }

}
