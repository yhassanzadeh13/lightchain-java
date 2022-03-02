package metrics;

import java.io.IOException;

import io.prometheus.client.exporter.HTTPServer;

/**
 * HTTP Server constructor class for the Prometheus exposer server.
 */
public class MetricServer {

  static HTTPServer server;

  /**
   * Initiates the Prometheus Exposer HTTP Server.
   */
  public static void start() {

    try {
      server = new HTTPServer(8081);
    } catch (IOException e) {
      throw new IllegalStateException("could not start metrics server:\t" + e);
    }

  }

  /**
   * Terminates the Prometheus Exposer HTTP Server.
   */
  public static void terminate() {

    try {
      server.stop();
    } catch (Exception e) {
      throw new IllegalStateException("could not stop metrics server:\t" + e);
    }

  }


}
