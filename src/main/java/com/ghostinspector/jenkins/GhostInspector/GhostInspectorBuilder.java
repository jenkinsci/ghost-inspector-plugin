package com.ghostinspector.jenkins.GhostInspector;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.Secret;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * GhostInspectorBuilder {@link Builder}.
 */
public class GhostInspectorBuilder extends Builder implements SimpleBuildStep {

  private static final String DISPLAY_NAME = "Run Ghost Inspector Test Suite";
  private static final String TEST_RESULTS_PASS = "pass";
  private static final int TIMEOUT = 36000;

  private final Secret apiKey;
  private final String suiteId;
  private final String startUrl;
  private final String params;

  @DataBoundConstructor
  public GhostInspectorBuilder(String apiKey, String suiteId, String startUrl, String params) {
    this.apiKey = Secret.fromString(apiKey);
    this.suiteId = suiteId;
    this.startUrl = startUrl;
    this.params = params;
  }

  /**
   * @return the apiKey
   */
  public Secret getApiKey() {
    return apiKey;
  }

  /**
   * @return the suiteId
   */
  public String getSuiteId() {
    return suiteId;
  }

  /**
   * @return the startUrl
   */
  public String getStartUrl() {
    return startUrl;
  }

  /**
   * @return additional parameters
   */
  public String getParams() {
    return params;
  }

  @Override
  public void perform(Run<?, ?> build, @Nonnull FilePath workspace, @Nonnull Launcher launcher, TaskListener listener)
      throws InterruptedException, IOException {
    PrintStream logger = listener.getLogger();
    EnvVars envVars = build.getEnvironment(listener);

    // Apply environment variables to parameters
    String expandedApiKey = "";
    if (apiKey != null) {
      expandedApiKey = envVars.expand(apiKey.getPlainText());
    }
    String expandedSuiteId = "";
    if (suiteId != null && !suiteId.isEmpty()) {
      expandedSuiteId = envVars.expand(suiteId);
    }
    String expandedStartUrl = "";
    if (startUrl != null && !startUrl.isEmpty()) {
      expandedStartUrl = envVars.expand(startUrl);
    }
    String expandedParams = "";
    if (params != null && !params.isEmpty()) {
      expandedParams = envVars.expand(params);
    }

    logger.println(DISPLAY_NAME);
    logger.println("Suite ID(s): " + expandedSuiteId);
    logger.println("Start URL: " + expandedStartUrl);
    logger.println("Additional Parameters: " + expandedParams);

    List<TestSuite> testSuiteList = TestSuite.buildFromCommaSeparatedList(expandedSuiteId);
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    Future<String> future = executorService.submit(
        new GhostInspectorTrigger(logger, expandedApiKey, testSuiteList, expandedStartUrl, expandedParams));

    try {
      String result = future.get(TIMEOUT + 30, TimeUnit.SECONDS);
      if (!TEST_RESULTS_PASS.equalsIgnoreCase(result)) {
        build.setResult(Result.FAILURE);
      }
    } catch (TimeoutException e) {
      logger.println("Timeout Exception:" + e.toString());
      build.setResult(Result.FAILURE);
      e.printStackTrace();
    } catch (Exception e) {
      logger.println("Exception:" + e.toString());
      build.setResult(Result.FAILURE);
      e.printStackTrace();
    }
    executorService.shutdownNow();
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  /**
   * Descriptor for {@link GhostInspectorBuilder}. Used as a singleton. The class
   * is marked as public so that it can be accessed from views.
   */
  @Extension
  @Symbol("ghostInspector")
  public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

    /**
     * In order to load the persisted global configuration, you have to call load()
     * in the constructor.
     */
    public DescriptorImpl() {
      load();
    }

    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      return true;
    }

    /**
     * This name is used in the configuration screen.
     */
    public String getDisplayName() {
      return DISPLAY_NAME;
    }

  }
}
