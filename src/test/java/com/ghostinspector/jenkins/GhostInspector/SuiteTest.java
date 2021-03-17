package com.ghostinspector.jenkins.GhostInspector;

import java.util.List;

import hudson.util.Secret;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class SuiteTest {

  private SuiteExecutionConfig config;

  @Test
  public void testInstantiation() {
    config = new SuiteExecutionConfig(Secret.fromString("api-key"), "suite-id", "my-start-url", "params=some-param");
    Suite suite = new Suite("suite-id", config);

    assertEquals(suite.id, "suite-id");
    assertEquals(suite.executeUrl, "https://api.ghostinspector.com/v1/suites/suite-id/execute?immediate=1&startUrl=my-start-url&params=some-param&apiKey=api-key");
    assertEquals(suite.safeExecuteUrl, "https://api.ghostinspector.com/v1/suites/suite-id/execute?immediate=1&startUrl=my-start-url&params=some-param&apiKey=api-xxx");
  }

  @Test
  public void testParseResultsSingle() {
    config = new SuiteExecutionConfig(Secret.fromString("api-key"), "suite-id", "my-start-url", "params=some-param");
    Suite suite = new Suite("suite-id", config);

    String rawResult = "{\"data\": {\"_id\": \"22233\"}}";
    List<SuiteResult> results = suite.parseResults(rawResult);
    assertEquals(results.size(), 1);
    assertEquals(results.get(0).id, "22233");
  }

  @Test
  public void testParseResultsMultiple() {
    config = new SuiteExecutionConfig(Secret.fromString("api-key"), "suite-id", "my-start-url", "params=some-param");
    Suite suite = new Suite("suite-id", config);

    String rawResult = "{\"data\": [{\"_id\": \"22233\"}, {\"_id\": \"33344\"}]}";
    List<SuiteResult> results = suite.parseResults(rawResult);
    assertEquals(results.size(), 2);
    assertEquals(results.get(0).id, "22233");
    assertEquals(results.get(1).id, "33344");
  }

}