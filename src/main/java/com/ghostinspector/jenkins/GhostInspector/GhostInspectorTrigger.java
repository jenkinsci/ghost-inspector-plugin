package com.ghostinspector.jenkins.GhostInspector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.List;

import net.sf.json.JSONObject;
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
    Logger.log("Executing:");
    
    // spin up each of the suites
    List<SuiteResult> suiteResults = new ArrayList<>();

    for (String suiteId : config.suiteIds) {
      Suite suite = new Suite(suiteId, config);
      Logger.log(" - executing URL: " + suite.safeExecuteUrl);

      String rawResponse = fetchUrl(suite.executeUrl);
      List<SuiteResult> newResults = suite.parseResults(rawResponse);
      Logger.log(" - suite [" + suiteId + "] returned " + newResults.size() + " results:");
      for (SuiteResult result: newResults) {
        suiteResults.add(result);
        Logger.log("  -> [" + result.id + "]");
      }
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

      Logger.log("Checking results:");
      for (SuiteResult suiteResult : suiteResults) {
        if (suiteResult.isComplete()) {
          continue;
        }

        // Check result
        String rawResult = fetchUrl(suiteResult.url);
        suiteResult.update(rawResult);
        Logger.log(" - result [" + suiteResult.id + "] status: passing " + suiteResult.getCountPassing() + " / failing: " + suiteResult.getCountFailing());

        if (suiteResult.isComplete()) {
          completeResults = completeResults + 1;
          Logger.log(" ✓ result complete [" + suiteResult.id + "] status: " + suiteResult.getStatus());
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

  /**
   * Method for making an HTTP call
   * 
   * @param url The URL to fetch
   * @return The response body of the URL
   */
  private String fetchUrl(String url) throws Exception {
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

        // Check for API errors
        JSONObject jsonResponse = JSONObject.fromObject(responseBody);
        String error = "";
        try {
          error = jsonResponse.getString("errorType");
        } catch (Exception ignored) {}

        if (!error.isEmpty()) {
          throw new SuiteExecutionException("API Error: " + jsonResponse.getString("message"));
        }
        

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
