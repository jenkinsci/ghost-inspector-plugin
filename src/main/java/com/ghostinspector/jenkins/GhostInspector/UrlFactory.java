package com.ghostinspector.jenkins.GhostInspector;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import hudson.EnvVars;
import hudson.util.Secret;

public class UrlFactory {

  private static final String apiRoot = "https://api.ghostinspector.com/v1";

  private String startUrl;
  private String urlParams;
  private Secret apiKey;
  private PrintStream logger;


  public UrlFactory(Secret apiKey, String startUrl, String urlParams) {
    this.apiKey = apiKey;
    this.startUrl = startUrl;
    this.urlParams = urlParams;
  }

  public void expandVariables(EnvVars envVars) {
    if (apiKey != null) {
      apiKey = Secret.fromString(envVars.expand(apiKey.getPlainText()));
    }

    if (startUrl != null && !startUrl.isEmpty()) {
      startUrl = envVars.expand(startUrl);
    }

    if (urlParams != null && !urlParams.isEmpty()) {
      urlParams = envVars.expand(urlParams);
    }
  }

  public String buildQueryString() {
    String queryString = "?immediate=1";
    if (startUrl != null && !startUrl.isEmpty()) {
      try {
        queryString = queryString + "&startUrl=" + URLEncoder.encode(startUrl, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        logger.println("WARN: Unable to urlencode startUrl: " + e.toString());
        queryString = queryString + "&starturl=" + startUrl;
      }
    }
    if (urlParams != null && !urlParams.isEmpty()) {
      queryString = queryString + "&" + urlParams;
    }

    return queryString;
  }

  public String getExecuteSuiteUrl(String suiteId) {
    return apiRoot + "/suites/" + suiteId + "/execute" + buildQueryString() + "&apiKey=" + apiKey.getPlainText();
  }

  public String getSafeExecuteSuiteUrl(String suiteId) {
    return apiRoot + "/suites/" + suiteId + "/execute" + buildQueryString() + "&apiKey=" + apiKey.getPlainText().substring(0, 4) + "xxx";
  }

  public String getSuiteResultUrl(String resultId) {
    return apiRoot + "/suite-results/" + resultId + "?apiKey=" + apiKey.getPlainText();
  }

  public String getUrlParams() {
    return urlParams;
  }

  public String getStartUrl() {
    return startUrl;
  }

  public void setLogger(PrintStream logger) {
    this.logger = logger;
  }

  public PrintStream getLogger() {
    return logger;
  }
}