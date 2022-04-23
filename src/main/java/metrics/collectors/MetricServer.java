package metrics.collectors;

import java.io.IOException;

import io.prometheus.client.exporter.HTTPServer;

/**
 * HTTP Server constructor class for the Prometheus exposer server.
 */
public class MetricServer {
  private HTTPServer server;
  private int SERVER_PORT;

  public MetricServer() {
    this.SERVER_PORT = 8081;
  }

  public MetricServer(int SERVER_PORT) {
    this.SERVER_PORT = SERVER_PORT;
  }

  /**
   * Initiates the Prometheus Exposer HTTP Server.
   */
  public void start() {

    try {
      server = new HTTPServer(SERVER_PORT);
    } catch (IOException e) {
      throw new IllegalStateException("could not start metrics server:\t" + e);
    }

  }

  /**
   * Terminates the Prometheus Exposer HTTP Server.
   */
  public void terminate() {
    try {
      server.close();
    } catch (Exception e) {
      throw new IllegalStateException("could not stop metrics server:\t" + e);
    }
  }

}
