Ghost Inspector Plugin for Jenkins
-------------
### Build status:
| Suite | Status |
| --- | --- |
| CircleCI plugin build | ![CircleCI](https://circleci.com/gh/ghost-inspector/ghost-inspector-jenkins-plugin/tree/stable.svg?style=svg) |
| Ghost Inspector suite | ![test suite](https://api.ghostinspector.com/v1/suites/5bb6451aa9f228640376c021/status-badge) |

With this plugin you can add a build step to your Jenkins project that executes a Ghost Inspector test suite. You can trigger this after deployment, or you can run the tests on a local build instance of your application using a tunnel in the "Start URL" field. If the test suite is successful, your pipeline will continue to the next step in your pipeline; however, if it fails (or times out), the build will be marked as failed.

## Installing from Jenkins
This plugin can be installed from within the Jenkins UI (recommended).

## Installing from Source
1. Download and unpack the source
2. From terminal ```mvn clean install```
3. Copy ```target/ghostinspector.hpi``` to ```$JENKINS_HOME/plugins```
4. Restart Jenkins

## Prerequisites
* **API Key** - This is a unique, private key provided with your account which allows you to access the API. You can find it in your Ghost Inspector account settings at https://app.ghostinspector.com/account.
* **Suite ID** - The ID of the Ghost Inpsector suite that you would like to execute.
 
## Usage
1. Open your project configuration from the Jenkins dashboard. 
2. In the build section, click ```Add build step``` and select ```Run Ghost Inspector Test Suite```.
3. In the ```API Key``` field, paste in your account's API key. For the ```Suite ID``` field, paste in the ID of the test suite that you would like to execute.
4. If you would like to run your Ghost Inspector tests on a URL other than their default setting (such as a local build instance of your application using a tunnel), enter the start URL in the ```Start URL``` field.
5. If you would like to pass other custom parameters or variables to your suite run, specify them in the ```Additional Parameters``` field.
6. Save your Jenkins project.

_Note:_ Environment variables may be used in both the ```Start URL``` and ```Additional Parameters``` field with the format ```$VAR_NAME```.

## Pipelines usage
The following example will allow you to trigger the Ghost Inspector plugin from your pipelines config:

```
node {
   stage('Main') {
        step([$class: 'GhostInspectorBuilder', apiKey: 'my-api-key', params: 'additional=parameters&another=one', startUrl: 'my-start-url', suiteId: 'my-suite-id'])
   }
}
```

## Development
Quick start:

```
$ brew install java8 maven
$ export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
$ mvn clean install
```

The resulting `ghost-inspector.hpi` can be found in the `./target` folder.

## Changelog
* 2020-Jun-29: Dependency updates; Increase timeout from 1hr to 10hrs; Apply environment variables to all fields.
* 2018-Oct-04: Update plugin for pipelines support
* 2018-Oct-04: Update plugin for pipelines support
* 2018-Feb-06: Add "Additional Parameters" field. Apply environment variables to "Start URL" and "Additional Parameters" fields.
* 2018-Feb-02: Move from sync API call to async polling
* 2017-Apr-04: Initial release 
