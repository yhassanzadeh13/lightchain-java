package integration.localnet;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.LogContainerResultCallback;

public class ContainerLogger {
  private static final int DEFAULT_LAST_LOG_TIME = 0;
  DockerClient dockerClient;
  private final ConcurrentHashMap<String, Integer> lastLogTime;

  public ContainerLogger(DockerClient dockerClient) {
    this.dockerClient = dockerClient;
    this.lastLogTime = new ConcurrentHashMap<>();
  }

  public void registerLogger(String containerId) {
    if (this.lastLogTime.containsKey(containerId)) {
      return;
    }
    this.lastLogTime.put(containerId, DEFAULT_LAST_LOG_TIME);
  }

  private LogContainerCmd createLogCommand(String containerId, int since) {
    return this.dockerClient
        .logContainerCmd(containerId)
        .withStdOut(true)
        .withStdErr(true)
        .withSince(since);
  }

  public void runContainerLoggerWorker() {
    for(Map.Entry<String, Integer> c : this.lastLogTime.entrySet()) {
      try(LogContainerCmd lg = createLogCommand(c.getKey(), c.getValue())) {
        lg.exec(new LogContainerResultCallback() {
          @Override
          public void onNext(Frame item) {
            super.onNext(item);
            System.out.println("[Container] " + new String(item.getPayload(), StandardCharsets.UTF_8));
            System.out.println("-------------------");
          }
        }).awaitCompletion();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      this.lastLogTime.put(c.getKey(), (int) (System.currentTimeMillis() / 1000));

//      lg.exec(new LogContainerResultCallback() {
//        @Override
//        public void onNext(Frame item) {
//          System.out.println("[Container] " + item.toString());
//        }
//      });
    }
  }
}
