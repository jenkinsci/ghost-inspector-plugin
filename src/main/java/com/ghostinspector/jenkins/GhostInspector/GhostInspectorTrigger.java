package com.ghostinspector.jenkins.GhostInspector;

import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.ghostinspector.jenkins.GhostInspector.TestSuite.Status.*;

/**
 * GhostInspectorTrigger
 */
public class GhostInspectorTrigger implements Callable<String> {

  private static final String API_HOST = "https://api.ghostinspector.com";
  private static final String API_VERSION = "v1";
  private static final String TEST_RESULTS_PASS = "pass";
  private static final String TEST_RESULTS_FAIL = "fail";

  private final Secret apiKey;
  private final List<TestSuite> testSuiteList;
  private final String startUrl;
  private final String params;

  private PrintStream log;

  public GhostInspectorTrigger(PrintStream logger, String apiKey, List<TestSuite> testSuiteList, String startUrl, String params) {
    this.log = logger;
    this.apiKey = Secret.fromString(apiKey);
    this.testSuiteList = testSuiteList;
    this.startUrl = startUrl;
    this.params = params;
  }

  @Override
  public String call() throws Exception {

    //populate URLs and ResultID for each test suite
    for (TestSuite ts : testSuiteList) {

      setExecuteUrl(ts);

      ts.setResultId( parseResultId(fetchUrl(ts.getExecuteUrl())));
      log.println("Suite triggered. Result ID received: " + ts.getResultId());

      ts.setResultUrl(API_HOST + "/" + API_VERSION + "/suite-results/" + ts.getResultId() + "/?apiKey=" + apiKey.getPlainText());
    }


    // Poll suite result until it completes
    while (true) {
      // Sleep for 10 seconds
      try {
        TimeUnit.SECONDS.sleep(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      for (TestSuite ts : testSuiteList) {
        if (isSuiteComplete(ts)) {
          continue;
        }
        ts.setStatus(parseResult(ts.getSuiteId(), fetchUrl(ts.getResultUrl())));
        if (isSuiteComplete(ts)) {
          log.println("Suite ID: " + ts.getSuiteId() + " has completed.");
        }
      }

      int suitesCompleted = countSuitesCompleted();
      if (suitesCompleted == testSuiteList.size()) {
        return getAllSuiteResult();
      }
      else {
        log.println("Suite(s) are still in progress. " +
                    "Completed " + suitesCompleted + " of " + testSuiteList.size() +
                    ". Checking again in 10 seconds...");
      }
    }
  }

  /**
   *  Look at all test suites, if one has failed, return fail
   * @return test suite status
   */
  private String getAllSuiteResult() {
    for (TestSuite ts : testSuiteList) {
      if (ts.getStatus() == COMPLETE_FAIL) {
        return TEST_RESULTS_FAIL;
      }
    }
    return TEST_RESULTS_PASS;
  }

  /**
   *  Set the execute URL on a single Test Suite Model
   * @param ts
   * @throws UnsupportedEncodingException
   */
  private void setExecuteUrl(TestSuite ts) throws UnsupportedEncodingException {
    // Generate suite execution API URL
    String executeUrl = API_HOST + "/" + API_VERSION + "/suites/" + ts.getSuiteId() + "/execute/?immediate=1";
    if (startUrl != null && !startUrl.isEmpty()) {
      executeUrl = executeUrl + "&startUrl=" + URLEncoder.encode(startUrl, "UTF-8");
    }
    if (params != null && !params.isEmpty()) {
      executeUrl = executeUrl + "&" + params;
    }
    log.println("Suite Execution URL: " + executeUrl);

    // Add API key after URL is logged
    ts.setExecuteUrl(executeUrl + "&apiKey=" + apiKey.getPlainText());
  }

  /**
   * Test to see if ALL suites have completed running, regardless of pass/fail
   * @return true if ALL suites have completed, false otherwise
   */
  private int countSuitesCompleted() {
    int count = 0;
    for (TestSuite tsm : testSuiteList) {
      if (isSuiteComplete(tsm)) {
        count++;
      }
    }
    return count;
  }

  /**
   * Test to see if suite has completed running, regardless of pass / fail
   * @param ts
   * @return true if complete, false otherwise
   */
  private boolean isSuiteComplete(TestSuite ts) {
    return ts.getStatus() == COMPLETE_PASS ||
           ts.getStatus() == COMPLETE_FAIL;
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
  private TestSuite.Status parseResult(String suiteId, String data) {
    JSONObject jsonObject = JSONObject.fromObject(data);
    JSONObject result = jsonObject.getJSONObject("data");

    if (result.get("passing").toString().equals("null")) {
      return PENDING;
    }

    log.println("Suite ID: " + suiteId);
    log.println("Test runs passed: " + result.get("countPassing"));
    log.println("Test runs failed: " + result.get("countFailing"));
    log.println("Execution time: " + (Integer.parseInt(result.get("executionTime").toString()) / 1000) + " seconds");

    if (result.get("passing").toString().equals("true")) {
      return COMPLETE_PASS;
    } else {
      return COMPLETE_FAIL;
    }
  }

  private static final Logger LOGGER = Logger.getLogger(GhostInspectorTrigger.class.getName());
}
