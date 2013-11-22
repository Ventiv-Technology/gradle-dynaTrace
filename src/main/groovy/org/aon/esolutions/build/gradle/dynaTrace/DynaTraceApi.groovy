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

import java.nio.charset.Charset;

import groovy.util.logging.Slf4j;

import org.aon.esolutions.build.gradle.dynaTrace.config.DynaTraceConfiguration
import org.aon.esolutions.build.gradle.dynaTrace.config.RecordSessionConfiguration;
import org.apache.tools.ant.util.Base64Converter
import org.gradle.api.Project

class DynaTraceApi {

	private DynaTraceConfiguration config;
	private Project project;
	private XmlSlurper slurper = new XmlSlurper();
	
	public DynaTraceApi(Project project, DynaTraceConfiguration config) {
		this.config = config;
		this.project = project;
	}
	
	public void setTestMetadataPerConfig() {
		Map<String, String> urlParameters = [:]
		
		String version = project.getVersion();
		String[] versionParts = version.split("\\.");
		if (versionParts.length > 0) urlParameters['versionmajor'] = versionParts[0]
		if (versionParts.length > 1) urlParameters['versionminor'] = versionParts[1]
		if (versionParts.length > 2) urlParameters['versionrevision'] = versionParts[2]
		if (versionParts.length > 3) urlParameters['versionmilestone'] = versionParts[3]
		if (versionParts.length > 4) urlParameters['versionbuild'] = versionParts[4]
		
		if (config.testRun.extraMetadata) {
			urlParameters << config.testRun.extraMetadata.findAll { it.value }
		}
		
		def result = executeRestfulCall("/setmetadata", "PUT", urlParameters);
		if (result.@value == false)
			throw new RuntimeException("Error returned from dynaTrace while setting Test Metadata, please check server logs")
	}
	
	public void startRecording(RecordSessionConfiguration config) {
		Map<String, String> urlParameters = [
			presentableName: config.getName(),
			description: config.getDescription(),
			isTimeStampAllowed: config.isTimestampIncluded().toString(),
			recordingOption: config.getRecordingOption(),
			isSessionLocked: config.isLockSession().toString(),
			label: config.getLabel()
		]
		
		def result = executeRestfulCall("/startrecording", "POST", urlParameters);
		if (result.@value) {
			project.getLogger().lifecycle("Recording Started: ${result.@value}");
		}
	}
	
	public void stopRecording() {
		def result = executeRestfulCall("/stoprecording");
		if (result.@value) {
			project.getLogger().lifecycle("Recording Stopped: ${result.@value}");
		}
	}
	
	private def executeRestfulCall(String path, String httpMethod = "GET", Map<String, String> urlParameters = [:]) {
		String url = config.agent.serverProtocol + "://" + config.agent.server + ":" + config.agent.serverPort +
			"/rest/management/profiles/" + config.testRun.profileName + path;
			
		String urlParametersStr = urlParameters ? urlParameters.collect { k, v ->
			return k + "=" + URLEncoder.encode(v, "UTF-8");
		}.join("&") : "";
			
		if (httpMethod == 'POST')
			return executeRestfulCallPost(url, urlParametersStr);
		else if (urlParameters)
			url = url + "?" + urlParametersStr;
			
		project.getLogger().debug("Calling dynaTrace API: $url");
		
		final HttpURLConnection conn = url.toURL().openConnection()
		conn.setRequestMethod(httpMethod);
		
		// Authentication
		if (config.agent.userName) {
			String encoded = new Base64Converter().encode(config.agent.userName + ":" + config.agent.password); 
			conn.setRequestProperty("Authorization", "Basic "+ encoded);
		}
		
		String response = conn.inputStream.getText();
		return slurper.parseText(response);
	}
	
	private def executeRestfulCallPost(String request, String urlParameters) {
		project.getLogger().debug("Calling dynaTrace API: $request");
		
		URL url = new URL(request);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setInstanceFollowRedirects(false);
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		connection.setRequestProperty("charset", "utf-8");
		connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
		connection.setUseCaches(false);
		
		if (config.agent.userName) {
			String encoded = new Base64Converter().encode(config.agent.userName + ":" + config.agent.password);
			connection.setRequestProperty("Authorization", "Basic "+ encoded);
		}
		
		byte[] data = urlParameters.getBytes(Charset.forName("UTF-8"));
		project.getLogger().debug("\tWith body: $urlParameters");
		
		connection.outputStream.write(data);		
		String response = connection.inputStream.getText();
				
		connection.disconnect();
		
		return slurper.parseText(response);
	}

}
