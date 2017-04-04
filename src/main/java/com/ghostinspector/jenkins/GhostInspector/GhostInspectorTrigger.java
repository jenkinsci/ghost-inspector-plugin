package com.ghostinspector.jenkins.GhostInspector;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URLEncoder;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * GhostInspectorTrigger
 *
 * @email help@ghostinspector.com
 */
public class GhostInspectorTrigger implements Callable<String> {

    private static final String API_HOST = "https://api.ghostinspector.com";
    private static final String APP_HOST = "https://app.ghostinspector.com";
    private static final String API_VERSION = "v1";
    private static final String TEST_RESULTS_PASS = "pass";
    private static final String TEST_RESULTS_FAIL = "fail";

    private final String apiKey;
    private final String suiteId;
    private final String startUrl;
    private final int timeout;

    private PrintStream log;
    String resp = null;

    public GhostInspectorTrigger(PrintStream logger, String apiKey, String suiteId, String startUrl, int timeout) {
        this.log = logger;
        this.apiKey = apiKey;
        this.suiteId = suiteId;
        this.startUrl = startUrl;
        this.timeout = timeout;
    }

    @Override
    public String call() throws Exception {
        String apiUrl = API_HOST + "/" + API_VERSION + "/suites/" + suiteId + "/execute/?apiKey=" + apiKey;
        if (startUrl != null && startUrl != "") {
            apiUrl = apiUrl + "&startUrl=" + URLEncoder.encode(startUrl, "UTF-8");
        }
        log.println("Suite Execution URL: " + apiUrl);

        resp = process(apiUrl);
        log.println("Response received: " + resp);

        return resp;
    }

    /**
     * Method for making HTTP call
     *
     * @param url
     * @return
     */
    public String process(String url) {
        String result = "";
        final CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault();
        try {
            httpclient.start();

            final RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(timeout * 1000)
                    .setConnectionRequestTimeout(timeout * 1000)
                    .setSocketTimeout(timeout * 1000).build();

            final HttpGet request = new HttpGet(url);
            request.setHeader("User-Agent", "ghost-inspector-jenkins-plugin/1.0");
            request.setConfig(config);
            final Future<HttpResponse> future = httpclient.execute(request, null);
            final HttpResponse response = future.get();

            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200 && statusCode != 201) {
                log.println(String.format("Error response from Ghost Inspector API, marking as failed: %s", statusCode));
            } else {
                String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                //log.println("Data received: " + responseBody);
                result = parseJSON(responseBody);
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
        return result;
    }


    /**
     * @param data
     * @return test result
     */
    private String parseJSON(String data) {
        int passing = 0;
        int failing = 0;
        JSONObject jsonObject = JSONObject.fromObject(data);
        JSONArray testResults = jsonObject.getJSONArray("data");

        for (int i = 0; i < testResults.size(); i++) {
            JSONObject test = testResults.getJSONObject(i);
            if (test.get("passing").toString() == "true") {
                passing++;
            } else {
                failing++;
            }
        }

        log.println("Test runs passed: " + passing);
        log.println("Test runs failed: " + failing);

        if (failing > 0) {
            return TEST_RESULTS_FAIL;
        } else {
            return TEST_RESULTS_PASS;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(GhostInspectorTrigger.class.getName());
}
