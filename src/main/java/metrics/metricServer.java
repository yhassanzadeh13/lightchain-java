package metrics;

import io.prometheus.client.exporter.HTTPServer;

import java.io.IOException;

public class metricServer {

    static HTTPServer server;

    public static void Start() {

        try {
            server = new HTTPServer(8081);
        } catch (IOException e) {
            throw new IllegalStateException("could not start metrics server:\t" + e);
        }

    }

    public static void Terminate() {

        try {
            server.stop();
        } catch (Exception e) {
            throw new IllegalStateException("could not stop metrics server:\t" + e);
        }

    }


}
