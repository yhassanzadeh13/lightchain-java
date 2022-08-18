package modules.logger;

import org.slf4j.LoggerFactory;

public class LighchainLogger {
  private static final String LOGGER_NAME = "io.github.yhassanzadeh13";

  public static org.slf4j.Logger getLogger(String className) {
    return LoggerFactory.getLogger(LOGGER_NAME + "." + className);
  }
}
