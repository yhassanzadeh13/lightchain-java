package metrics.collectors;

import java.io.IOException;

import io.prometheus.client.exporter.HTTPServer;

/**
 * HTTP Server constructor class for the Prometheus exposer server.
 */
public class MetricServer {
  private HTTPServer server;
  private int serverPort;

  public MetricServer() {
    this.serverPort = 8081;
  }

  public MetricServer(int serverPort) {
    this.serverPort = serverPort;
  }

  /**
   * Initiates the Prometheus Exposer HTTP Server.
   */
  public void start() {

    try {
      server = new HTTPServer(serverPort);
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
