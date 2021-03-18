package com.ghostinspector.jenkins.GhostInspector;

import java.io.PrintStream;

public class Logger {
  private static PrintStream logger;

  public static void log(String message) {
    logger.println(message);
  }

  public static void setLogger(PrintStream incoming) {
    logger = incoming;
  }
}