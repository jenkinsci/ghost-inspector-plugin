package com.ghostinspector.jenkins.GhostInspector;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URLEncoder;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

import net.sf.json.JSONObject;

/**
 * GhostInspectorTrigger
 */
public class GhostInspectorTrigger implements Callable<String> {

  private static final String API_HOST = "https://api.ghostinspector.com";
  private static final String API_VERSION = "v1";
  private static final String TEST_RESULTS_PENDING = "pending";
  private static final String TEST_RESULTS_PASS = "pass";
  private static final String TEST_RESULTS_FAIL = "fail";

  private final String apiKey;
  private final String suiteId;
  private final String startUrl;
  private final String params;

  private PrintStream log;

  public GhostInspectorTrigger(PrintStream logger, String apiKey, String suiteId, String startUrl, String params) {
    this.log = logger;
    this.apiKey = apiKey;
    this.suiteId = suiteId;
    this.startUrl = startUrl;
    this.params = params;
  }

  @Override
  public String call() throws Exception {
    String result = null;
    // Generate suite execution API URL
    String executeUrl = API_HOST + "/" + API_VERSION + "/suites/" + suiteId + "/execute/?immediate=1";
    if (startUrl != null && !startUrl.isEmpty()) {
      executeUrl = executeUrl + "&startUrl=" + URLEncoder.encode(startUrl, "UTF-8");
    }
    if (params != null && !params.isEmpty()) {
      executeUrl = executeUrl + "&" + params;
    }
    log.println("Suite Execution URL: " + executeUrl);

    // Add API key after URL is logged
    executeUrl = executeUrl + "&apiKey=" + apiKey;

    // Trigger suite and fetch result ID
    String resultId = parseResultId(fetchUrl(executeUrl));
    log.println("Suite triggered. Result ID received: " + resultId);

    // Poll suite result until it completes
    String resultUrl = API_HOST + "/" + API_VERSION + "/suite-results/" + resultId + "/?apiKey=" + apiKey;
    while (true) {
      // Sleep for 10 seconds
      try {
        TimeUnit.SECONDS.sleep(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      // Check result
      result = parseResult(fetchUrl(resultUrl));
      if (result == TEST_RESULTS_PENDING) {
        log.println("Suite is still in progress. Checking again in 10 seconds...");
      } else {
        return result;
      }
    }
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
        log.println(String.format("Error response from Ghost Inspector API, marking as failed: %s", statusCode));
      } else {
        responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
        // log.println("Data received: " + responseBody);
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Exception: ", e);
      e.printStackTrace();
    } finally {
      try {
        httpclient.close();
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Error closing connection: ", e);
        e.printStackTrace();
      }
    }
    return responseBody;
  }

  /**
   * Parse the suite result ID from API JSON response
   *
   * @param data The JSON to parse in string format
   * @return The ID of the suite result
   */
  private String parseResultId(String data) {
    JSONObject jsonObject = JSONObject.fromObject(data);
    JSONObject result = jsonObject.getJSONObject("data");
    return result.get("_id").toString();
  }

  /**
   * Parse the suite result JSON response to determine status
   *
   * @param data The JSON to parse in string format
   * @return The status of the suite result
   */
  private String parseResult(String data) {
    JSONObject jsonObject = JSONObject.fromObject(data);
    JSONObject result = jsonObject.getJSONObject("data");

    if (result.get("passing").toString().equals("null")) {
      return TEST_RESULTS_PENDING;
    }

    log.println("Test runs passed: " + result.get("countPassing"));
    log.println("Test runs failed: " + result.get("countFailing"));
    log.println("Execution time: " + (Integer.parseInt(result.get("executionTime").toString()) / 1000) + " seconds");

    if (result.get("passing").toString().equals("true")) {
      return TEST_RESULTS_PASS;
    } else {
      return TEST_RESULTS_FAIL;
    }
  }

  private static final Logger LOGGER = Logger.getLogger(GhostInspectorTrigger.class.getName());
}
