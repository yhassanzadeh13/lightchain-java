package metrics;

/**
 * Collector of metrics for the LightChain implementation of Prometheus equivalents.
 */
public class LightChainCollector implements Collector {

  @Override
  public HistogramCollector histogram() {
    return new LightChainHistogram();
  }

  @Override
  public GaugeCollector gauge() {
    return new LightChainGauge();
  }

  @Override
  public CounterCollector counter() {
    return new LightChainCounter();
  }
}
