# LightChain Local Network

Runs a local network of LightChain nodes (with 5 nodes by default). Each node is running in a separate container, `NODE0` to `NODE4`.
Additionally, there is a Grafana and Prometheus container to monitor the network. All nodes are reporting metrics to Prometheus, 
which is then used by Grafana to display the metrics in a dashboard.

To start first run:
```bash
make docker-build-lightchain
```

Then run the Cmd file in this package. It will start the network and open the Grafana dashboard in your browser.
Grafana is available on `localhost:3000` and Prometheus on `localhost:9090`. The default username and password for Grafana is `admin:admin`.

To stop the network kill the Cmd process and then run:
```bash 
make docker-clean-lightchain
```
This will stop and remove all containers together with their volumes.

Finally, to stop the Prometheus and Grafana containers run:
```bash
make docker-stop-metrics
```
This stops the containers but does not remove them. We don't recommend removing them as it will remove all dashboards and data. If
needed you can force remove them by docker commands.