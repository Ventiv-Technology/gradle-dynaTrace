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
package org.aon.esolutions.build.gradle.dynaTrace.config

class DynaTraceConfiguration {

	AgentConfiguration agent = new AgentConfiguration();
	TestConfiguration testRun = new TestConfiguration();
	
	def agent(Closure closure) {
		closure.resolveStrategy = Closure.DELEGATE_FIRST
		closure.setDelegate(agent);
		closure();
	}
	
	def testRun(Closure closure) {
		closure.resolveStrategy = Closure.DELEGATE_FIRST
		closure.setDelegate(testRun);
		closure();
	}
	
	def dynaTrace(Closure closure) {
		closure.resolveStrategy = Closure.DELEGATE_FIRST
		closure.setDelegate(this);
		closure();
	}

}
