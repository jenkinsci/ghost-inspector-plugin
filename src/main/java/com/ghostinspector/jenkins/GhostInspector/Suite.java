package com.ghostinspector.jenkins.GhostInspector;

import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import com.ghostinspector.jenkins.GhostInspector.SuiteResult;

public class Suite {
  public final String id;
  public final String executeUrl;
  public final String safeExecuteUrl;

  private final SuiteExecutionConfig config;

  public Suite (String id, SuiteExecutionConfig config) {
    this.id = id;
    this.executeUrl = config.urls.getExecuteSuiteUrl(id);
    this.safeExecuteUrl = config.urls.getSafeExecuteSuiteUrl(id);
    this.config = config;
  }

  public List<SuiteResult> parseResults(String rawData) {
    List<SuiteResult> suiteResults = new ArrayList<>();

    JSONObject rawResults = JSONObject.fromObject(rawData);
    try {
      JSONObject result = rawResults.getJSONObject("data");
      String id = result.get("_id").toString();
      SuiteResult suiteResult = new SuiteResult(id, config);
      suiteResults.add(suiteResult);
    } catch (JSONException e) {
      // rawResults must be a list
      JSONArray results = rawResults.getJSONArray("data");
      for (int index = 0 ; index < results.size(); index++) {
        JSONObject result = results.getJSONObject(index);
        String id = result.get("_id").toString();
        SuiteResult suiteResult = new SuiteResult(id, config);
        suiteResults.add(suiteResult);
      }
    }

    return suiteResults;
  }
}