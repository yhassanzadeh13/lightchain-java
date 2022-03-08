package integration.localnet;

import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.api.model.Ports;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Main {

  public static void main(String[] args) throws InterruptedException {

    System.out.println("f");

    DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

    DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .maxConnections(100)
            .connectionTimeout(Duration.ofSeconds(30))
            .responseTimeout(Duration.ofSeconds(45))
            .build();

    DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);

    // Volume check + create if absent

//    ListVolumesResponse volumesResponse = dockerClient.listVolumesCmd().exec();
//    List<InspectVolumeResponse> volumes = volumesResponse.getVolumes();
//
//    boolean volumeFound = false;
//
//    for (InspectVolumeResponse v : volumes) {
//      System.out.println(v.getName());
//    }
//
//    CreateVolumeResponse volume;
//    if(!volumeFound) {
//      volume = dockerClient.createVolumeCmd().withName("volume").exec();
//    }

    // Prometheus
//
//    dockerClient.pullImageCmd("prom/prometheus")
//            .withTag("main")
//            .exec(new PullImageResultCallback())
//            .awaitCompletion(30, TimeUnit.SECONDS);
//
//    Ports promPortBindings = new Ports();
//    promPortBindings.bind(ExposedPort.tcp(9090), Ports.Binding.bindPort(9090));
//
//    CreateContainerResponse promContainer =
//            dockerClient
//                    .createContainerCmd("prom/prometheus:main")
//                    .withTty(true)
//                    .withPortBindings(promPortBindings)
//                    .exec();
//
//    dockerClient
//            .startContainerCmd(promContainer.getId())
//            .exec();
//
//    // Grafana
//
//    dockerClient.pullImageCmd("grafana/grafana")
//            .withTag("main")
//            .exec(new PullImageResultCallback())
//            .awaitCompletion(30, TimeUnit.SECONDS);
//
//    Ports grafanaPortBindings = new Ports();
//    grafanaPortBindings.bind(ExposedPort.tcp(3000), Ports.Binding.bindPort(3000));
//
//    CreateContainerResponse grafanaContainer =
//            dockerClient
//                    .createContainerCmd("grafana/grafana:main")
//                    .withTty(true)
//                    .withPortBindings(grafanaPortBindings)
//                    .exec();
//
//    dockerClient
//            .startContainerCmd(grafanaContainer.getId())
//            .exec();

    // HTTP Server

    String imageId = dockerClient.buildImageCmd()
            .withDockerfile(new File("src/main/java/integration/localnet/Dockerfile"))
            .withPull(true)
            .exec(new BuildImageResultCallback())
            .awaitImageId();

    Ports serverPortBindings = new Ports();
    serverPortBindings.bind(ExposedPort.tcp(8080), Ports.Binding.bindPort(8080));

    CreateContainerResponse serverContainer =
            dockerClient
                    .createContainerCmd(imageId)
                    .withTty(true)
                    .withPortBindings(serverPortBindings)
                    .exec();

    dockerClient
            .startContainerCmd(serverContainer.getId())
            .exec();




  }

}
