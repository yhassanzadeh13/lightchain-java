package metrics.integration;

import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.github.dockerjava.api.*;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.*;
import com.github.dockerjava.httpclient5.*;
import com.github.dockerjava.transport.*;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import metrics.LightChainCollector;
import metrics.MetricServer;

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
    MetricsTestNet testNet = new MetricsTestNet();

    try {
      testNet.run();
    } catch (IllegalStateException e) {
      System.err.println("could not run metrics testnet" + e);
      System.exit(1);
    }


    // Metric Server Initiation
    try {

      collector = new LightChainCollector();

      finalizedBlockCount = collector.counter().register("finalized_block_count",
              "consensus", "proposal", "Finalized block count");

      currentBlockCount = collector.gauge().register("current_block_count",
              "consensus", "proposal", "Finalized block count");

    } catch (IllegalArgumentException ex) {
      System.err.println("Could not initialize the metrics with the provided arguments" + ex.toString());
      System.exit(1);
    }

    try {
      MetricServer.start();
    } catch (IllegalStateException ex) {
      System.err.println("Could not start the Metric Server");
      System.exit(1);
    }

    while (true) {
      try {
        Thread.sleep(1000);
        finalizedBlockCount.inc(1);
        currentBlockCount.inc(1);
      } catch (InterruptedException ex) {
        System.err.println("Thread sleep issue, breaking the loop");
        break;
      }
    }

    try {
      MetricServer.terminate();
    } catch (IllegalStateException ex) {
      System.err.println("Could not stop the Metric Server");
      System.exit(1);
    }

  }

}
