package integration.localnet;

import java.io.IOException;

import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;

public class DemoServer {

  static HTTPServer server;

  static final Counter finalizedBlockCount = Counter.build()
          .name("consensus_proposal_finalized_block_count").help("Finalized block count").register();
  static final Gauge currentBlockCount = Gauge.build()
          .name("consensus_proposal_current_block_count").help("Current block count").register();

  public static void main(String[] args) {

    System.out.println("yoo");

    try {
      server = new HTTPServer(8080);
    } catch (IOException e) {
      throw new IllegalStateException("could not start metrics server:\t" + e);
    }

    finalizedBlockCount.inc(32);
    currentBlockCount.inc(12);

  }

}
