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
  private Secret apiKeySecret;
  private PrintStream logger;


  public UrlFactory(Secret apiKeySecret, String startUrl, String urlParams) {
    this.apiKeySecret = apiKeySecret;
    this.startUrl = startUrl;
    this.urlParams = urlParams;
  }

  public void expandVariables(EnvVars envVars) {
    if (apiKeySecret != null) {
      apiKeySecret = Secret.fromString(envVars.expand(apiKeySecret.getPlainText()));
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
    return apiRoot + "/suites/" + suiteId + "/execute" + buildQueryString() + "&apiKey=" + apiKeySecret.getPlainText();
  }

  public String getSafeExecuteSuiteUrl(String suiteId) {
    return apiRoot + "/suites/" + suiteId + "/execute" + buildQueryString() + "&apiKey=" + apiKeySecret.getPlainText().substring(0, 4) + "xxx";
  }

  public String getSuiteResultUrl(String resultId) {
    return apiRoot + "/suite-results/" + resultId + "?apiKey=" + apiKeySecret.getPlainText();
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