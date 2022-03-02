package metrics;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;

/**
 * Demonstrative class to set up a Prometheus server and create LightChain Counter and Gauge instances.
 */
public class Demo {

  static LightChainCollector collector = new LightChainCollector();
  static Counter finalizedBlockCount = collector.counter().register("finalized_block_count",
          "consensus", "proposal", "Finalized block count");
  static Gauge currentBlockCount = collector.gauge().register("current_block_count",
          "consensus", "proposal", "Finalized block count");

  /**
   * main function.
   *
   * @param args standart Java args
   */
  public static void main(String[] args) {

    MetricServer.start();
    finalizedBlockCount.inc(32);
    currentBlockCount.inc(12);

  }

}
