package integration.localnet;

import java.util.*;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.LogContainerResultCallback;

public class ContainerLogger {
  private static final int DEFAULT_LAST_LOG_TIME = 0;
  DockerClient dockerClient;
  private final HashMap<String, Integer> lastLogTime;

  public ContainerLogger(DockerClient dockerClient) {
    this.dockerClient = dockerClient;
    this.lastLogTime = new HashMap<>();
  }

  public void registerLogger(String containerId) {
    if (this.lastLogTime.containsKey(containerId)) {
      return;
    }
    this.lastLogTime.put(containerId, DEFAULT_LAST_LOG_TIME);
  }

  private LogContainerCmd createLogCommand(String containerId, int since) {
    LogContainerCmd logContainerCmd = this.dockerClient
        .logContainerCmd(containerId)
        .withStdOut(true)
        .withStdErr(true)
        .withSince(since)
        .withTail(1);
    logContainerCmd.withStdOut(true).withStdErr(true);

    return logContainerCmd;
  }

  public void runContainerLoggerWorker() {
    for(Map.Entry<String, Integer> c : this.lastLogTime.entrySet()) {
      LogContainerCmd lg = createLogCommand(c.getKey(), c.getValue());
      this.lastLogTime.put(c.getKey(), (int) (System.currentTimeMillis() / 1000));

      lg.exec(new LogContainerResultCallback() {
        @Override
        public void onNext(Frame item) {
          System.out.println("[Container] " + item.toString());
        }
      });
    }
  }
}
