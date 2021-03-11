package com.ghostinspector.jenkins.GhostInspector;

public class Suite {
  public final String id;
  public final String executeUrl;
  public final String safeExecuteUrl;

  public Suite (String id, SuiteExecutionConfig config) {
    this.id = id;
    this.executeUrl = config.urls.getExecuteSuiteUrl(id);
    this.safeExecuteUrl = config.urls.getSafeExecuteSuiteUrl(id);
  }
}