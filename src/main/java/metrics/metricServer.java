package metrics;

import io.prometheus.client.exporter.HTTPServer;
import java.io.IOException;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;

public class metricServer {

        static final Counter finalizedBlockCount = Counter.build()
                .name("consensus_proposal_finalized_block_count").help("Finalized block count").register();
        static final Gauge currentBlockCount = Gauge.build()
                .name("consensus_proposal_current_block_count").help("Current block count").register();

        static HTTPServer server;

        public static void main(String[] args) {

            Start();

        }

    public static void Start() {

        finalizedBlockCount.inc(35);
        currentBlockCount.inc(15);

        try {
            server = new HTTPServer(8081);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void Terminate() {
        server.stop();
    }




}
