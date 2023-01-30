import org.junit.Test;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class PrometheusTest {

  private final String PROMETHEUS_URL = "http://localhost:9090/graph";
  private final String METRIC_NAME = "my_metric";

  @Test
  public void testPrometheusMetric() throws IOException {
    // Query the /graph endpoint of Prometheus
    URL url = new URL(PROMETHEUS_URL);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");

    // Check that the metric has been registered
    String response = new Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A").next();
    assert response.contains(METRIC_NAME);

    double prevValue = 0;
    for (int i = 0; i < 10; i++) {
      // Read the value of the metric
      double currentValue = readMetricValue(METRIC_NAME);
      assert currentValue > prevValue;
      prevValue = currentValue;
    }
  }

  private double readMetricValue(String metricName) throws IOException {
    // Query the /metrics endpoint of Prometheus
    URL url = new URL(PROMETHEUS_URL + "/metrics");
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");

    // Find the value of the specified metric
    String response = new Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A").next();
    int index = response.indexOf(metricName);
    String value = response.substring(index + metricName.length() + 2).split(" ")[0];
    return Double.parseDouble(value);
  }
}
