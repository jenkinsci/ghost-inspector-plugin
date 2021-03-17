package com.ghostinspector.jenkins.GhostInspector;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.*;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.Secret;
import jenkins.tasks.SimpleBuildStep;
import javax.annotation.Nonnull;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;


/**
 * GhostInspectorBuilder {@link Builder}.
 */
public class GhostInspectorBuilder extends Builder implements SimpleBuildStep {

  private static final String displayName = "Run Ghost Inspector Test Suite";
  private static final int TIMEOUT = 36000;
  private SuiteExecutionConfig config;

  private final Secret apiKey;
  private final String suiteId;
  private final String startUrl;
  private final String params;

  @DataBoundConstructor
  public GhostInspectorBuilder(String apiKey, String suiteId, String startUrl, String params) {
    // store these for display in Jenkins
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
    
    // set up logger
    Logger.setLogger(listener.getLogger());

    // set up initial configuration for execution
    config = new SuiteExecutionConfig(apiKey, suiteId, startUrl, params);
    config.applyVariables(build.getEnvironment(listener));

    // report our status before we start
    reportExecutionConfiguration();

    ExecutorService executorService = Executors.newSingleThreadExecutor();
    
    try {
      Future<String> future = executorService.submit(new GhostInspectorTrigger(config));
      String finalStatus = future.get(TIMEOUT + 30, TimeUnit.SECONDS);
      if (!ResultStatus.Passing.equals(finalStatus)) {
        build.setResult(Result.FAILURE);
      }
    } catch (TimeoutException e) {
      Logger.log("Timeout Exception:" + e.toString());
      build.setResult(Result.FAILURE);
      e.printStackTrace();
    } catch (Exception e) {
      String message = e.getMessage();
      if (message.contains("API Error")) {
        Logger.log(message);
      } else {
        Logger.log("Exception:" + e.toString());
        e.printStackTrace();
      }
      build.setResult(Result.FAILURE);
    }
    executorService.shutdownNow();
  }

  private void reportExecutionConfiguration() {
    Logger.log("");
    Logger.log("#####################################");
    Logger.log(displayName);
    Logger.log("#####################################");
    Logger.log("");
    Logger.log("Configuration:");
    Logger.log(" - id(s): " + String.join(", ", config.suiteIds));
    Logger.log(" - startUrl: " + config.getStartUrl());
    Logger.log(" - params: " + config.getUrlParams());
    Logger.log("");
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
      return displayName;
    }

  }
}
