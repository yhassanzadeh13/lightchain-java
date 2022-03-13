package metrics;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;

/**
 * Demonstrative class to set up a Prometheus server and create LightChain Counter and Gauge instances.
 */
public class Demo {

  static LightChainCollector collector;
  static Counter finalizedBlockCount;
  static Gauge currentBlockCount;

  /**
   * main function.
   *
   * @param args standart Java args
   */
  public static void main(String[] args) {

    try {

      collector = new LightChainCollector();

      finalizedBlockCount = collector.counter().register("finalized_block_count",
              "consensus", "proposal", "Finalized block count");

      currentBlockCount = collector.gauge().register("current_block_count",
              "consensus", "proposal", "Finalized block count");

    } catch (Exception ex) {
      System.err.println("Could not initialize the metrics");
    }

    try {
      MetricServer.start();
    } catch (Exception ex) {
      System.err.println("Could not start the Metric Server");
    }

    while (true) {
      try {
        Thread.sleep(1000);
        finalizedBlockCount.inc(1);
        currentBlockCount.inc(1);
      } catch (InterruptedException ex) {
        System.err.println("Thread sleep issue");
        Thread.currentThread().interrupt();
        break;
      }
    }

  }

}
