package com.ghostinspector.jenkins.GhostInspector;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

import hudson.util.Secret;

public class SuiteResultTest {

  private SuiteExecutionConfig config;
  private SuiteResult suiteResult;

  @Before
  public void init() {
    config = new SuiteExecutionConfig(Secret.fromString("api-key"), "suite-id", "my-start-url", "params=true");
    suiteResult = new SuiteResult("1234567890", config);
  }

  @Test
  public void testInstantiation() {
    assertEquals(suiteResult.id, "1234567890");
    assertEquals(suiteResult.url, "https://api.ghostinspector.com/v1/suite-results/1234567890?apiKey=api-key");
    assertEquals(suiteResult.getStatus(), ResultStatus.Pending);
    assertEquals(suiteResult.getCountFailing(), "0");
    assertEquals(suiteResult.getCountPassing(), "0");
    assertEquals(suiteResult.getExecutionTime(), "0");
    assertEquals(suiteResult.isComplete(), false);
    assertEquals(suiteResult.isPassing(), false);
  }

  @Test
  public void testUpdate() {
    String newData = "{\"data\":{\"passing\": true, \"countPassing\": 1, \"countFailing\": 2, \"executionTime\": 5000}}";
    suiteResult.update(newData);
    assertEquals(suiteResult.id, "1234567890");
    assertEquals(suiteResult.getStatus(), ResultStatus.Passing);
    assertEquals(suiteResult.getCountFailing(), "2");
    assertEquals(suiteResult.getCountPassing(), "1");
    assertEquals(suiteResult.getExecutionTime(), "5");
    assertEquals(suiteResult.isComplete(), true);
    assertEquals(suiteResult.isPassing(), true);
  }
}