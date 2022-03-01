package metrics;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;

public class demo {

    static LightChainCollector collector = new LightChainCollector();
    static Counter finalizedBlockCount = collector.counter().register("finalized_block_count", "consensus", "proposal", "Finalized block count");
    static Gauge currentBlockCount = collector.gauge().register("current_block_count", "consensus", "proposal", "Finalized block count");


    public static void main(String[] args) {

        metricServer.Start();
        finalizedBlockCount.inc(32);
        currentBlockCount.inc(12);

    }

}
