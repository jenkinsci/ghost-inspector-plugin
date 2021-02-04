package com.ghostinspector.jenkins.GhostInspector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Holder class for all suites to be run, each entry from comma separated entered list will create one instance
 */
public class TestSuite {

  private String suiteId, resultId;

  private String executeUrl, resultUrl;

  private Status status;

  enum Status {
    PENDING,
    COMPLETE_PASS,
    COMPLETE_FAIL
  }

  /**
   * Creates a Test Suite with default status
   * @param suiteId one suite ID from input list
   */
  public TestSuite(String suiteId) {
    this.suiteId = suiteId;
    this.status = Status.PENDING;
  }

  /**
   * @param suiteList this list of suite IDs input from the UI
   * @return populated test suites with opening Status of PENDING, empty list if suiteList is null or empty
   */
  public static List<TestSuite> buildFromCommaSeparatedList(String suiteList) {
    if (suiteList == null || suiteList.isEmpty()) {
      return Collections.emptyList();
    }
    //remove all whitespace
    suiteList = suiteList.replaceAll("\\s+","");
    List<String> suiteStringList = Arrays.asList(suiteList.split("\\,"));

    List<TestSuite> testSuiteList = new ArrayList<>();
    for (String suiteId : suiteStringList) {
      testSuiteList.add(new TestSuite(suiteId));
    }
    return testSuiteList;
  }

  public String getSuiteId() {
    return suiteId;
  }

  public String getResultId() {
    return resultId;
  }

  public void setResultId(String resultId) {
    this.resultId = resultId;
  }

  public String getExecuteUrl() {
    return executeUrl;
  }

  public void setExecuteUrl(String executeUrl) {
    this.executeUrl = executeUrl;
  }

  public String getResultUrl() {
    return resultUrl;
  }

  public void setResultUrl(String resultUrl) {
    this.resultUrl = resultUrl;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }
}
