# Gradle dynaTrace Plugin

This is a simple plugin to help integrate gradle with dynaTrace.

## Configuration

The first thing you need to configure is the dependency on this build library.  The common way would be to download the JAR from the GitHub releases, and then add it into some sort of maven repository (such as Artifactory or Nexus).  Then, in your build file add the following:

	buildscript {
		dependencies {
			classpath fileTree(dir: 'build/libs', include: '*.jar')
		}
	}

	apply plugin: 'dynaTrace'

Of course, the classpath indicated here needs to point to the JAR, being in a maven repository, or on the local filesystem (as shown above).

Next, you must configure where the dynaTrace plugin with a dynaTrace closure.  Please see pluginTest for a good example that will be discussed here.

It is important to note here that you can configure the agent from gradle properties.  Please see 14.2 on [http://www.gradle.org/docs/current/userguide/tutorial_this_and_that.html](http://www.gradle.org/docs/current/userguide/tutorial_this_and_that.html "Gradle - This and That") to get an example.  In the example you might do `gradle -PdynaTraceServer=somebuildserver.com test`.  If you do not supply the parameter, the server gets set to null, and the plugin is disabled.

NOTE: If you use properties, and any of the REQUIRED properties are missing, the plugin will disable, and no integration will happen

## Configuration Options
- dynaTrace - REQUIRED - Root level configuration object
	- agent - REQUIRED - Configures the dynaTrace agent that is installed on the build machine
		- path - REQUIRED - The location of the agent library
		- name - REQUIRED - The name of the agent to show up in dynaTrace
		- server - REQUIRED - URL for the server (e.g. dynapp01.int.com).  This should be where the RESTful endpoints are
		- serverProtocol - Protocol to use for RESTful calls.  Defaults to http.
		- collector - URL for for the dynaTrace Collector (e.g. dyncol01.int.com).  This is optional, and will default to the server
		- serverPort - Port for the RESTful services.  Defaults to 8020
		- collectorPort - Port for the collector.  Defaults to 9998 
		- extraParameters - Hash for extra parameters to pass to the agent.  See [https://community.compuwareapm.com/community/display/DOCDT55/Java+Agent+Configuration](https://community.compuwareapm.com/community/display/DOCDT55/Java+Agent+Configuration "Java Agent Configuration")
	- testRun
		- profileName - REQUIRED - Name of the profile that lines up with the name of the agent.  TODO: Automatically figure this out via restful calls
		- extraMetadata - Optionally passes extra metadata to dynaTrace to help keep track of things.  Good examples would be buildNumber or vcsRevision.
		- recordSession - If present, the session will be recorded in dynaTrace
			- name - REQUIRED - Name to give the session.  This is the name that will show up in the "Session Storage" section in the dynaTrace Cockpit
			- description - More detailed description.  Accessible in the 'details' of the recorded session.  Defaults to blank
			-   timestampIncluded - Append the timestamp on the recorded session name?  Defaults to false
			-   recordingOption - 'all' = All Purepaths.  'violations' = only PurePaths marked as violated and time series.  'timeseries' =  time series only.  Defaults to 'all'
			-   lockSession - Lock this session from being deleted.  Defaults to false.
			-   label - Label to append to the test.  Defaults to 'UnitTest' 

## Notes on Spock Testing

Out of the box, dynaTrace only has sensors for JUnit testing.  In order to get Spock tests to work, you will need to annotate each test with the JUnit @Test.  This plugin has limited support to help.  If you add a spockTests configuration to the dynaTrace gradle configuration, you can be alerted or fail the build if the annotation is missing.  Here is an example:

	dynaTrace {
		spockTests {
			junitTestAnnotationLevel = 'FAIL'
		}
	}

junitTestAnnotationLevel can be any one of 'FAIL', 'DEBUG', 'INFO', 'LIFECYCLE', 'WARN', 'QUIET', 'ERROR'.  These correspond to the various levels of logging in Gradle, with the exception of 'FAIL'.  This special setting will fail the build, as well as log at an ERROR level.

This will add a new task 'verifySpockJunitTestAnnotation' that will only execute if the output level matches what is requested.  For example, if you have it set to 'DEBUG', but are not running with the --debug flag, then this task will not execute.