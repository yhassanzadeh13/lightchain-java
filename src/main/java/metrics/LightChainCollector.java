package metrics;

/**
 * Collector of metrics for the LightChain implementation of Prometheus equivalents.
 */
public class LightChainCollector implements Collector {

  HistogramCollector lightChainHistogram;
  GaugeCollector lightChainGauge;
  CounterCollector lightChainCounter;

  public LightChainCollector() {
    this.lightChainHistogram = new LightChainHistogram();
    this.lightChainGauge = new LightChainGauge();
    this.lightChainCounter = new LightChainCounter();
  }

  @Override
  public HistogramCollector histogram() {
    return this.lightChainHistogram;
  }

  @Override
  public GaugeCollector gauge() {
    return this.lightChainGauge;
  }

  @Override
  public CounterCollector counter() {
    return this.lightChainCounter;
  }

}
