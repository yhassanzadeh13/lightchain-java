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

  private final Hashtable<String, String> loggingBuffer;

  /**
   * Minimum length of a log to be printed.
   */
  private static final int MIN_LOG_LENGTH = 30;
  private final Logger logger = LightchainLogger.getLogger(ContainerLogger.class.getCanonicalName());

  public ContainerLogger(DockerClient dockerClient) {
    this.dockerClient = dockerClient;
    this.lastLogTime = new ConcurrentHashMap<>();
    this.loggingBuffer = new Hashtable<>();
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
        .withSince(since);
  }

  public void runContainerLoggerWorker() {
    for(Map.Entry<String, Integer> c : this.lastLogTime.entrySet()) {
      try(LogContainerCmd lg = createLogCommand(c.getKey(), c.getValue())) {
        lg.exec(new ResultCallback.Adapter<>() {
          @Override
          public void onNext(Frame item) {
            super.onNext(item);
            String lg = new String(item.getPayload(), StandardCharsets.UTF_8);
            String[] split = lg.split("\n");
            System.out.println("-------------------");
            for(String s : split) {
                System.out.println("[Container] " + s);
            }
            System.out.println("-------------------");
            // inspectLog(c.getKey(), lg);
          }
        }).awaitCompletion();
      } catch (InterruptedException e) {
        logger.fatal("error while logging container: {}", c.getKey(), e);
      }
      this.lastLogTime.put(c.getKey(), (int) (System.currentTimeMillis() / 1000));
    }
  }

  LogContainerResultCallback loggingCallback = new
      LogContainerResultCallback();

  /**
   * Buffers the log message from the container.
   *
   * @param containerId the container id.
   * @param log the log message.
   */
  private void bufferLog(String containerId, String log) {
    if (this.loggingBuffer.containsKey(containerId)) {
      this.loggingBuffer.put(containerId, this.loggingBuffer.get(containerId) + log);
    } else {
      this.loggingBuffer.put(containerId, log);
    }
  }

  /**
   * Flushes the log message from the container.
   *
   * @param containerId the container id.
   */
  private void flushLog(String containerId) {
    if (this.loggingBuffer.containsKey(containerId)) {
      this.logger.info("node_id {}: {}", containerId, this.loggingBuffer.get(containerId));
      this.loggingBuffer.remove(containerId);
    }
  }

  /**
   * Logs the log message from the container.
   *
   * @param containerId the container id.
   * @param log the log message.
   */
  private void inspectLog(String containerId, String log) {
    log = log.trim();
    if (log.length() < MIN_LOG_LENGTH) {
      bufferAndFlushLog(containerId, log);
    } else {
      flushLog(containerId);
      this.logger.info("node_id {}: {}", containerId, log);
    }
  }

  /**
   * Buffers the log message from the container and flushes if the buffered log message is
   * longer than the minimum length.
   *
   * @param containerId the container id.
   * @param log the log message.
   */
  private void bufferAndFlushLog(String containerId, String log) {
    bufferLog(containerId, log);
    if(this.loggingBuffer.get(containerId).length() > MIN_LOG_LENGTH) {
      flushLog(containerId);
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