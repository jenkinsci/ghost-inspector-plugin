package com.ghostinspector.jenkins.GhostInspector;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;

import hudson.EnvVars;
import hudson.util.Secret;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class UrlFactoryTest {

  private UrlFactory urls;

  @Before
  public void init() {
    urls = new UrlFactory(Secret.fromString("api-key"), "https://start.url", "url=params&another=one");
  }

  @Test
  public void testGetters() {
    assertEquals(urls.getUrlParams(), "url=params&another=one");
    assertEquals(urls.getStartUrl(), "https://start.url");
    assertEquals(urls.getExecuteSuiteUrl("12345"), "https://api.ghostinspector.com/v1/suites/12345/execute?immediate=1&startUrl=https%3A%2F%2Fstart.url&url=params&another=one&apiKey=api-key");
    assertEquals(urls.getSafeExecuteSuiteUrl("12345"), "https://api.ghostinspector.com/v1/suites/12345/execute?immediate=1&startUrl=https%3A%2F%2Fstart.url&url=params&another=one&apiKey=api-xxx");
    assertEquals(urls.getSuiteResultUrl("23456"), "https://api.ghostinspector.com/v1/suite-results/23456?apiKey=api-key");
    assertEquals(urls.getUrlParams(), "url=params&another=one");
    assertEquals(urls.getStartUrl(), "https://start.url");
    // check logger
    PrintStream logger = new PrintStream(new ByteArrayOutputStream());
    urls.setLogger(logger);
    assertEquals(urls.getLogger() instanceof PrintStream, true);
  }

  @Test
  public void testBuildQueryString() {
    String queryString = urls.buildQueryString();
    assertEquals(queryString, "?immediate=1&startUrl=https%3A%2F%2Fstart.url&url=params&another=one");
  }

  @Test
  public void testExpandVariables() {
    UrlFactory _urls = new UrlFactory(Secret.fromString("api-${varOne}-key"), "https://${varTwo}.url", "url=params&another=${varThree}");

    HashMap<String, String> map = new HashMap<String, String>();
    map.put("varOne", "my");
    map.put("varTwo", "hello");
    map.put("varThree", "world");

    EnvVars envVars = new EnvVars(map);
    _urls.expandVariables(envVars);

    String url = _urls.getExecuteSuiteUrl("1234");
    assertEquals(url, "https://api.ghostinspector.com/v1/suites/1234/execute?immediate=1&startUrl=https%3A%2F%2Fhello.url&url=params&another=world&apiKey=api-my-key");
  }
}
