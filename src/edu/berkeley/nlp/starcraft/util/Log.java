package edu.berkeley.nlp.starcraft.util;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.spi.LoggingEvent;
import org.bwapi.proxy.model.Game;

public class Log {
  private static final String TIMESTAMP = new SimpleDateFormat(
      "yyyyMMdd'T'HHmmss").format(new GregorianCalendar().getTime());
  private static final String DEFAULT_CONSOLE_LOG_FILE = "Log files/" + "overmind-console_"
      + TIMESTAMP + ".log";
  private static final String DEFAULT_LOG_FILE = "Log files/" + "overmind_" + TIMESTAMP
      + ".log";
  public static final String LOG_KEY = "logFile";

  private static Map<String, Log> logs = new HashMap<String, Log>();

  private static String consoleLogFile = DEFAULT_CONSOLE_LOG_FILE;
  private static String logFile = DEFAULT_LOG_FILE;

  private Logger consoleLogger;
  private Logger fileLogger;

  public static Log getLog(String name) {
    Log log = logs.get(name);
    if (log == null) {
      log = new Log(name);
      logs.put(name, log);
    }
    return log;
  }

  private Log(String name) {
    if (name.length() > 0) {
      this.consoleLogger = Logger.getLogger("console." + name);
      this.fileLogger = Logger.getLogger("file." + name);
    } else {
      this.consoleLogger = Logger.getLogger("console");
      this.fileLogger = Logger.getLogger("file");
    }
  }

  public static void initLogger(Level consoleLevel, Level fileLevel) throws IOException {
    BwapiAppender bwapiAppender = new BwapiAppender();
    bwapiAppender.setLayout(new OvermindLayout(true));
    Logger.getLogger("console").addAppender(bwapiAppender);
    FileAppender consoleAppender = new FileAppender(new OvermindLayout(), consoleLogFile, false);
    consoleAppender.setBufferedIO(false);
    consoleAppender.setBufferSize(4096);
		Logger.getLogger("console").addAppender(
        consoleAppender);
    FileAppender fileAppender = new FileAppender(new OvermindLayout(), logFile, false);
    fileAppender.setBufferedIO(true);
    fileAppender.setBufferSize(4096);
		Logger.getLogger("file").addAppender(
        fileAppender);
    setConsoleLevel("", consoleLevel);
    setFileLevel("", fileLevel);
  }

  public static void setConsoleLevel(String name, Level level) {
    if (name.length() > 0)
      Logger.getLogger("console." + name).setLevel(level);
    else
      Logger.getLogger("console").setLevel(level);
  }

  public static void setFileLevel(String name, Level level) {
    if (name.length() > 0)
      Logger.getLogger("file." + name).setLevel(level);
    else
      Logger.getLogger("file").setLevel(level);
  }

  public static Collection<String> getLogNames() {
    return Collections.unmodifiableSet(logs.keySet());
  }

  public void log(Priority priority, java.lang.Object message) {
    consoleLogger.log(priority, message);
    fileLogger.log(priority, message);
  }
  
  public void log(Priority priority, java.lang.Object message, Throwable t) {
    consoleLogger.log(priority, message, t);
    fileLogger.log(priority, message, t);
  }

  public void trace(java.lang.Object message) {
    log(Level.TRACE, message);
  }

  public void debug(java.lang.Object message) {
    log(Level.DEBUG, message);
  }

  public void info(java.lang.Object message) {
    log(Level.INFO, message);
  }

  public void warn(java.lang.Object message) {
    log(Level.WARN, message);
  }

  public void error(java.lang.Object message) {
    log(Level.ERROR, message);
  }

  public void fatal(java.lang.Object message) {
    log(Level.FATAL, message);
  }

  public void fatal(java.lang.Object message, Throwable t) {
    log(Level.FATAL, message, t);
  }

  public static class OvermindLayout extends SimpleLayout {

    boolean truncate;

    public OvermindLayout() {
      this(false);
    }

    public OvermindLayout(boolean truncate) {
      super();
      this.truncate = truncate;
    }

    @Override
    public String format(LoggingEvent event) {
      String frames = Integer.toString(Game.getInstance().getFrameCount());
      String[] parts = event.getLoggerName().split("[.]");
      String name = String.format("%s" + (20 - frames.length()),
          parts[parts.length - 1]);
      String output = "[" + frames + ":" + name + "] " + super.format(event);
      /*
      if(event.getThrowableStrRep() != null)
        for(String s: event.getThrowableStrRep()) {
          output += "\n\t" + s;
        }
        */

      // To avoid overflow we truncate the message length
      // TODO: adjust the code so that we split up long messages rather than
      // truncate.
      if (truncate)
        return output.substring(0, Math.min(240, output.length()));
      else
        return output;
    }
  }

  public static void setLogFile(String logFileName) {
    logFile = logFileName;
  }

}