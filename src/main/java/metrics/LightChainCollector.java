package metrics;

/**
 * Collector of metrics for the LightChain implementation of Prometheus equivalents.
 */
public class LightChainCollector implements Collector {

  HistogramCollector lightChainHistogram = new LightChainHistogram();
  GaugeCollector lightChainGauge = new LightChainGauge();
  CounterCollector lightChainCounter = new LightChainCounter();

  @Override
  public HistogramCollector histogram() {
    return lightChainHistogram;
  }

  @Override
  public GaugeCollector gauge() {
    return lightChainGauge;
  }

  @Override
  public CounterCollector counter() {
    return lightChainCounter;
  }

}
