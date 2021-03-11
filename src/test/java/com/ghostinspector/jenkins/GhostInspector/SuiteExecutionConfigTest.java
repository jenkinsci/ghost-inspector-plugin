package com.ghostinspector.jenkins.GhostInspector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import hudson.EnvVars;

public class SuiteExecutionConfigTest {
  
  @Test
  public void testInstantiation() {
    SuiteExecutionConfig config = new SuiteExecutionConfig("my-api-key", "my-suite-id", "my-start-url", "param=one");
    List<String> expected = Arrays.asList("my-suite-id");
    assertEquals(config.suiteIds, expected);

    // should instantiate urls object internally
    assertEquals(config.urls instanceof UrlFactory, true);

    // check the values are passed through
    assertEquals(config.urls.getStartUrl(), "my-start-url");
    assertEquals(config.urls.getUrlParams(), "param=one");
  }

  @Test
  public void testMultipleSuiteIds() {
    SuiteExecutionConfig config = new SuiteExecutionConfig("my-api-key", "suite-one, suite-two", "my-start-url", "param=one");
    List<String> expected = Arrays.asList("suite-one", "suite-two");
    assertEquals(config.suiteIds, expected);

    // should instantiate urls object internally
    assertEquals(config.urls instanceof UrlFactory, true);
  }


  @Test
  public void testExpandVariables() {
    // confirm the variables are applied to the underlying class
    SuiteExecutionConfig config = new SuiteExecutionConfig("api-${varOne}-key", "1234", "https://${varTwo}.url", "url=params&another=${varThree}");
    HashMap<String, String> map = new HashMap<String, String>();
    map.put("varOne", "my");
    map.put("varTwo", "hello");
    map.put("varThree", "world");

    EnvVars envVars = new EnvVars(map);
    config.applyVariables(envVars);

    String url = config.urls.getExecuteSuiteUrl("1234");
    assertEquals(url, "https://api.ghostinspector.com/v1/suites/1234/execute?immediate=1&startUrl=https%3A%2F%2Fhello.url&url=params&another=world&apiKey=api-my-key");
  }
}
