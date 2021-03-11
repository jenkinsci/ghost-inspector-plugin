package com.ghostinspector.jenkins.GhostInspector;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class SuiteTest {

  private SuiteExecutionConfig config;

  @Test
  public void testInstantiation() {
    config = new SuiteExecutionConfig("api-key", "suite-id", "my-start-url", "params=some-param");
    Suite suite = new Suite("suite-id", config);

    assertEquals(suite.id, "suite-id");
    assertEquals(suite.executeUrl, "https://api.ghostinspector.com/v1/suites/suite-id/execute?immediate=1&startUrl=my-start-url&params=some-param&apiKey=api-key");
    assertEquals(suite.safeExecuteUrl, "https://api.ghostinspector.com/v1/suites/suite-id/execute?immediate=1&startUrl=my-start-url&params=some-param&apiKey=api-xxx");
  }

}