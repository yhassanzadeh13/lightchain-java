package integration.localnet;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import modules.logger.LightchainLogger;
import modules.logger.Logger;

public class ContainerLogger {
  private static final int DEFAULT_LAST_LOG_TIME = 0;
  DockerClient dockerClient;
  private final ConcurrentHashMap<String, Integer> lastLogTime;
  private final Logger logger = LightchainLogger.getLogger(ContainerLogger.class.getCanonicalName());

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
        .withFollowStream(true)
        .withStdOut(true)
        .withStdErr(true)
        .withSince(0);
  }

  public void runContainerLoggerWorker() {
    for(Map.Entry<String, Integer> c : this.lastLogTime.entrySet()) {
      try(LogContainerCmd lg = createLogCommand(c.getKey(), c.getValue())) {
        lg.exec(new ResultCallback.Adapter<>() {
          @Override
          public void onNext(Frame item) {
            // super.onNext(item);
            String log = new String(item.getPayload(), StandardCharsets.UTF_8);
            // logger.info("{}: {}", c.getKey(), log);
            System.out.printf("%s: %s \n", c.getKey(), log);
          }
        }).awaitCompletion(1, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        logger.fatal("error while logging container: {}", c.getKey(), e);
      }
      this.lastLogTime.put(c.getKey(), (int) (System.currentTimeMillis() / 1000));
    }
  }
}

//  private void runCmd(Instruction instruction, Map<String, String> s) {
//    String containerId = client.createContainerCmd(s.get("image")).withCmd(s.get("cmd").split(" ")).exec().getId();
//    client.startContainerCmd(containerId).exec();
//
//    LogContainerCmd logContainerCmd = client.logContainerCmd(containerId);
//    logContainerCmd.withStdOut(true).withStdErr(true);
//    StringBuffer sb = new StringBuffer();
//
//    try {
//      logContainerCmd.exec(new ResultCallback.Adapter<Frame>() {
//        @Override
//        public void onNext(Frame item) {
//          sb.append(item.toString());
//        }
//      }).awaitCompletion(10 ,TimeUnit.MINUTES);
//      addInstructionLog(instruction.getId(), sb.toString());
//    } catch (InterruptedException e) {
//      addInstructionLog(instruction.getId(), "error：" + e);
//    }
//
//    client.removeContainerCmd(containerId).exec();
//  }