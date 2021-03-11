package com.ghostinspector.jenkins.GhostInspector;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

/**
 * GhostInspectorTrigger
 */
public class GhostInspectorTrigger implements Callable<String> {

  private final SuiteExecutionConfig config;
  // private PrintStream log;

  public GhostInspectorTrigger(SuiteExecutionConfig config) {
    this.config = config;
    // this.log = config.getLogger();
  }

  @Override
  public String call() throws Exception {
    
    // spin up each of the suites
    List<SuiteResult> suiteResults = Collections.emptyList();

    for (String suiteId : config.suiteIds) {
      Suite suite = new Suite(suiteId, config);
      Logger.log("Suite Execution URL: " + suite.safeExecuteUrl);

      String rawResult = fetchUrl(suite.executeUrl);

      // TODO: here we will check for multiple results
      SuiteResult suiteResult = new SuiteResult(rawResult, config);
      Logger.log("Suite triggered, result ID: " + suiteResult.id);

      suiteResults.add(suiteResult);
    }

    // check the results to see where we're at
    int totalResults = suiteResults.size();
    int completeResults = 0;
    while (completeResults < totalResults) {
      try {
        TimeUnit.SECONDS.sleep(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      for (SuiteResult suiteResult : suiteResults) {
        if (suiteResult.isComplete()) {
          continue;
        }

        // Check result
        String rawResult = fetchUrl(suiteResult.url);
        suiteResult.update(rawResult);
        reportResultStatus(suiteResult);

        if (suiteResult.isComplete()) {
          completeResults = completeResults + 1;
          Logger.log(" ... suite result " + suiteResult.id + " is complete.");
        }
      }
    }

    // all results are finished, report aggregated results
    return checkAllResults(suiteResults);
  }

  private String checkAllResults(List<SuiteResult> results) {
    for (SuiteResult result : results) {
      if (result.getStatus() == ResultStatus.Failing) {
        return ResultStatus.Failing;
      }
    }
    return ResultStatus.Passing;
  }

  private void reportResultStatus(SuiteResult suiteResult) {
    Logger.log("Test runs passed: " + suiteResult.getCountPassing());
    Logger.log("Test runs failed: " + suiteResult.getCountFailing());
    Logger.log("Execution time: " + suiteResult.getExecutionTime() + " seconds");
  }

  /**
   * Method for making an HTTP call
   * 
   * @param url The URL to fetch
   * @return The response body of the URL
   */
  private String fetchUrl(String url) {
    String responseBody = "";
    final CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault();
    try {
      httpclient.start();

      final RequestConfig config = RequestConfig.custom().setConnectTimeout(60 * 1000)
          .setConnectionRequestTimeout(60 * 1000).setSocketTimeout(60 * 1000).build();

      final HttpGet request = new HttpGet(url);
      request.setHeader("User-Agent", "ghost-inspector-jenkins-plugin/1.0");
      request.setConfig(config);
      final Future<HttpResponse> future = httpclient.execute(request, null);
      final HttpResponse response = future.get();

      final int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != 200 && statusCode != 201) {
        Logger.log(String.format("Error response from Ghost Inspector API, marking as failed: %s", statusCode));
      } else {
        responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
        // Logger.log("Data received: " + responseBody);
      }
    } catch (Exception e) {
      Logger.log("Exception: " + e.getMessage());
      e.printStackTrace();
    } finally {
      try {
        httpclient.close();
      } catch (IOException e) {
        Logger.log("Error closing connection: " + e.getMessage());
        e.printStackTrace();
      }
    }
    return responseBody;
  }
}
