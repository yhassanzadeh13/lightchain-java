package metrics;

import io.prometheus.client.Counter;

public class LightChainCounter implements CounterCollector{
    @Override
    public Counter register(String name, String namespace, String subsystem, String helpMessage) throws IllegalArgumentException {
        return Counter.build().name(namespace+"_"+subsystem+"_"+name).help(helpMessage).register();
    }
}
