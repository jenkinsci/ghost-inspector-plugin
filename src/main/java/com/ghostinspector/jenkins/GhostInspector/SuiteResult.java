package com.ghostinspector.jenkins.GhostInspector;

import net.sf.json.JSONObject;

public class SuiteResult {
  public final String id;
  public final String url;

  private String status;
  private String countPassing = "0";
  private String countFailing = "0";
  private String executionTime = "0";

  public SuiteResult(String id, SuiteExecutionConfig config) {
    this.status = ResultStatus.Pending;
    this.id = id;
    this.url = config.urls.getSuiteResultUrl(this.id);
  }

  public String getStatus() {
    return status;
  }

  public String getCountPassing() {
    return countPassing;
  }

  public String getCountFailing() {
    return countFailing;
  }

  public String getExecutionTime() {
    return executionTime;
  }

  public Boolean isComplete() {
    return status != ResultStatus.Pending;
  }

  public Boolean isPassing() {
    return status == ResultStatus.Passing;
  }

  public void update(String rawData) {
    JSONObject parsed = JSONObject.fromObject(rawData).getJSONObject("data");
    String newStatus = parsed.get("passing").toString();

    if (newStatus.equals("null")) {
      status = ResultStatus.Pending;
    } else if (newStatus.equals("true")) {
      status = ResultStatus.Passing;
    } else {
      status = ResultStatus.Failing;
    }

    // set counts
    countPassing = parsed.get("countPassing").toString();
    countFailing = parsed.get("countFailing").toString();

    // set execution time
    // TODO: test checking for "null"
    String rawExecutionTime = parsed.get("executionTime").toString();
    if (!rawExecutionTime.equals("null")) {
      executionTime = String.valueOf(Integer.parseInt(rawExecutionTime) / 1000);
    }
  }

  // private String parseId(String rawData) {
  //   JSONObject parsed = JSONObject.fromObject(rawData).getJSONObject("data");
  //   return parsed.get("_id").toString();
  // }
}
