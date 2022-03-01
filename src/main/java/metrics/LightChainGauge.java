package metrics;

import io.prometheus.client.Gauge;

public class LightChainGauge implements GaugeCollector{
    @Override
    public Gauge register(String name, String namespace, String subsystem, String helpMessage) throws IllegalArgumentException {
        return Gauge.build().name(namespace+"_"+subsystem+"_"+name).help(helpMessage).register();
    }
}
