package com.ghostinspector.jenkins.GhostInspector;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import hudson.EnvVars;
import hudson.util.Secret;

public class SuiteExecutionConfig {

  public final List<String> suiteIds;
  public final UrlFactory urls;

  public SuiteExecutionConfig(String apiKey, String rawIdString, String startUrl, String params) {
    this.suiteIds = parseIds(rawIdString);
    this.urls = new UrlFactory(Secret.fromString(apiKey), startUrl, params);
  }

  public void applyVariables(EnvVars variables) {
    urls.expandVariables(variables);
  }

  public void setLogger(PrintStream logger) {
    urls.setLogger(logger);
  }

  public PrintStream getLogger() {
    return urls.getLogger();
  }

  public String getStartUrl() {
    return urls.getStartUrl();
  }

  public String getUrlParams() {
    return urls.getUrlParams();
  }

  private List<String> parseIds(String rawIdString) {
    if (rawIdString == null || rawIdString.isEmpty()) {
      // TODO: this should throw an error?
      return Collections.emptyList();
    }
    //remove all whitespace
    rawIdString = rawIdString.replaceAll("\\s+","");
    List<String> ids = Arrays.asList(rawIdString.split("\\,"));
    return ids;
  }
}
