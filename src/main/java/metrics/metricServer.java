package metrics;

import io.prometheus.client.exporter.HTTPServer;

import java.io.IOException;

public class metricServer {

    static HTTPServer server;

    public static void Start() {

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
