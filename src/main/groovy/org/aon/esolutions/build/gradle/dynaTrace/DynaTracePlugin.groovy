/**
 * Copyright (c) 2013 Aon eSolutions
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.aon.esolutions.build.gradle.dynaTrace

import org.aon.esolutions.build.gradle.dynaTrace.config.DynaTraceConfiguration
import org.aon.esolutions.build.gradle.dynaTrace.spock.SpockTestAnnotationHarness
import org.aon.esolutions.build.gradle.dynaTrace.spock.SpockTestAnnotationTransform
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.TaskState
import org.gradle.api.tasks.testing.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

public class DynaTracePlugin implements Plugin<Project>  {
	
	public static final Logger log = LoggerFactory.getLogger(DynaTracePlugin.class)
	
	private DynaTraceConfiguration config = new DynaTraceConfiguration()
	private DynaTraceApi api;

	@Override
	public void apply(Project project) {
		project.convention.plugins.dynaTrace = config
		api = new DynaTraceApi(project, config);

        // When the task Graph is ready, Find the test task and do the following:
        // - Set the JVM args to start dynaTrace instrumentation
        // - Start Recording - if configuration is there
        project.afterEvaluate {
            Test testTask = project.getTasks().getByName(JavaPlugin.TEST_TASK_NAME) as Test
            if (testTask) {
                // Add the verifySpockJunitTestAnnotation Task
                Task junitTestAnnotationVerification = null;
                LogLevel juniTestAnnotationVerificationLogLevel = getJunitTestAnnotationLogLevel();
                if (juniTestAnnotationVerificationLogLevel == null || project.getLogger().isEnabled(juniTestAnnotationVerificationLogLevel)) {
                    junitTestAnnotationVerification = project.task("verifySpockJunitTestAnnotation")
                    junitTestAnnotationVerification.ext.logLevel = juniTestAnnotationVerificationLogLevel
                    junitTestAnnotationVerification << this.&junitTestAnnotationVerificationTask
                    testTask.dependsOn junitTestAnnotationVerification
                }

                // Add the task to start communications with dynaTrace
                if (isConfigurationComplete()) {
                    // Add the dynaTraceTestSetup Task
                    Task dynaTraceTestSetup = project.task("dynaTraceTestSetup")
                    dynaTraceTestSetup << this.&dynaTraceTestSetupTask
                    testTask.dependsOn dynaTraceTestSetup
                    if (junitTestAnnotationVerification) dynaTraceTestSetup.dependsOn junitTestAnnotationVerification
                }
            }
        }

        // Attach a 'listener' to the tasks finishing, so we can stop recording
		project.gradle.taskGraph.afterTask { Task task, TaskState state ->
			if (state.failure) {
				if (isSessionRecording()) api.stopRecording();
			}
			else if (task == project.gradle.taskGraph.getAllTasks().last()) {
				if (isSessionRecording()) api.stopRecording();
			}
		}
	}

    private dynaTraceTestSetupTask(Task t) {
        Test testTask = t.getProject().getTasks().getByName(JavaPlugin.TEST_TASK_NAME) as Test

        String agentPath = "-agentpath:${config.agent.path}=name=${config.agent.name},server=${config.agent.getCollector()}:${config.agent.collectorPort}";
        if (config.agent.extraParameters)
            agentPath += "," + config.agent.extraParameters.collect { k, v -> k + "=" + v }.join(",")

        testTask.setJvmArgs([agentPath]);

        api.setTestMetadataPerConfig();
        if (isSessionRecording()) api.startRecording(config.testRun.recordSession);
    }

    private junitTestAnnotationVerificationTask(Task t) {
        Project project = t.getProject();
        def testTask = project.getTasks().getByName(JavaPlugin.TEST_TASK_NAME)
        Collection<File> testClassPath = testTask.classpath.getFiles();

        SpockTestAnnotationHarness harness = new SpockTestAnnotationHarness(testClassPath, project.property('sourceSets').test.groovy.getFiles());
        harness.scanClasses();
        int numberMissing = SpockTestAnnotationTransform.printProblemMethods(project.getLogger(), t.ext.logLevel);
        if (numberMissing && config?.spockTests?.junitTestAnnotationLevel?.equalsIgnoreCase("FAIL"))
            throw new InvalidUserDataException("${numberMissing} Spock Test(s) do not have @Test annotation, and build is configured to FAIL");
    }

    public LogLevel getJunitTestAnnotationLogLevel() {
        String logLevelConfig = config?.spockTests?.junitTestAnnotationLevel;

        if (logLevelConfig?.equalsIgnoreCase("FAIL"))
            return LogLevel.ERROR;
        else if (logLevelConfig)
            return LogLevel.valueOf(logLevelConfig);
        else
            return null;
    }
	
	public boolean isConfigurationComplete() {
		return config.agent?.path?.trim()?.length() > 0 &&
		       config.agent?.name?.trim()?.length() > 0 &&
			   config.agent?.server?.trim()?.length() > 0 &&
			   config.testRun?.profileName?.trim()?.length() > 0
	}
	
	public boolean isSessionRecording() {
		return config.testRun?.recordSession?.name?.length() > 0;
	}
}
