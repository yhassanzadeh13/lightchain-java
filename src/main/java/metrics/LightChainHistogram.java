package metrics;

import io.prometheus.client.Histogram;

public class LightChainHistogram implements HistogramCollector{

    @Override
    public Histogram register(String name, String namespace, String subsystem, String helpMessage, double[] buckets) throws IllegalArgumentException {
        return Histogram.build().name(namespace+"_"+subsystem+"_"+name).help(helpMessage).register();
    }

}
