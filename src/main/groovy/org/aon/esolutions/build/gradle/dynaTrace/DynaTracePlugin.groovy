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
package org.aon.esolutions.build.gradle.dynaTrace;

import org.aon.esolutions.build.gradle.dynaTrace.config.DynaTraceConfiguration
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.tasks.TaskState
import org.gradle.api.tasks.testing.Test;
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
		
		project.gradle.taskGraph.whenReady { TaskExecutionGraph graph ->
			Test testTask = graph.getAllTasks().find { Task t -> t.getName() == 'test' }
			if (testTask) {
				if (isConfigurationComplete()) {
					api.setTestMetadataPerConfig();
					
					String agentPath = "-agentpath:${config.agent.path}=name=${config.agent.name},server=${config.agent.getCollector()}:${config.agent.collectorPort}";
					if (config.agent.extraParameters)
						agentPath += "," + config.agent.extraParameters.collect { k, v -> k + "=" + v }.join(",")
						
					testTask.setJvmArgs([agentPath]);
				}
			}
		}
		
		project.gradle.taskGraph.afterTask { Task task, TaskState state ->
			if (state.failure) {
				println "FAILED"
			}
			else if (task == project.gradle.taskGraph.getAllTasks().last()) {
				println "DONE!!!"
			}
		}
	}
	
	public boolean isConfigurationComplete() {
		return config.agent?.path?.trim()?.length() > 0 &&
		       config.agent?.name?.trim()?.length() > 0 &&
			   config.agent?.server?.trim()?.length() > 0 &&
			   config.testRun?.profileName?.trim()?.length() > 0
	}  
}
