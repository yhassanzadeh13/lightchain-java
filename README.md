# LightChain (Java)

Reference implementation of the Lightchain blockchain protocol in Java. Note that this is still a work-in-progress with
many features still missing. Also, although we aim a robust and safe implementation, this is a _research project_ and
should not be used in production either as a library or as a standalone application.

## Building

```shell
$ make compile-lightchain 
```

## Running the tests

```shell
$ make test
```

## Linter and formatter
This project uses [SpotBugs](https://spotbugs.github.io/) for static analysis and [Checkstyle](https://checkstyle.sourceforge.io/) for code style checking.
To have a clean codebase, we recommend running the following commands before submitting a pull request. The remote CI will also run these checks.

```shell
$ make lint
```

```shell
$ make sptbug-check
```

## Local Testnet
Local testnet is still a work-in-progress. It eventually will allow you to run a local network of LightChain nodes
each on a separate container. This network will be used for testing and development purposes. It also enables
logging and metrics (Grafana + Prometheus) for monitoring the network.

At the moment, you can run a network of nodes using the following command. However, since Lightchain node is still under
development, the local testnet spins up instances of a mock node ([Node.java](src%2Fmain%2Fjava%2Fbootstrap%2FNode.
java)) that is capable of sending and receiving Hello messages.

To start first run:
```bash
make docker-build-lightchain
```
This bootstraps ([Bootstrap.java](src%2Fmain%2Fjava%2Fbootstrap%2FBootstrap.java)) the network by creating public and 
private keys for each node and building the Docker images.

Then run the Cmd ([Cmd.java](src%2Fmain%2Fjava%2Fintegration%2Flocalnet%2FCmd.java)) file in this package. It will 
start the network and open the Grafana dashboard in your browser.
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


