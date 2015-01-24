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
package org.aon.esolutions.build.gradle.dynaTrace.spock

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.classgen.GeneratorContext
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.Phases
import org.codehaus.groovy.control.SourceUnit

import java.security.CodeSource

/**
 *
 *
 * @author John Crygier
 */
class SpockTestAnnotationHarness extends GroovyClassLoader {

    private Collection<File> classpath;
    private Collection<File> filesToScan;
    private List<String> errorMessages = [];

    public SpockTestAnnotationHarness(Collection<File> classpath, Collection<File> filesToScan) {
        super();
        this.filesToScan = filesToScan;
        this.classpath = classpath;
    }

    public List<String> scanClasses() {
        classpath.each { this.addClasspath(it.getAbsolutePath()) }

        this.filesToScan.each {
            try {
                this.parseClass(it);
            } catch (Throwable e) {
                errorMessages << "Unable to parse $it, so there may be @Test annotations missing"
            }
        }

        return errorMessages;
    }

    protected CompilationUnit createCompilationUnit(CompilerConfiguration config, CodeSource codeSource) {
        CompilationUnit cu = super.createCompilationUnit(config, codeSource)
        cu.addPhaseOperation(new SpockTestAnnotationHarnessOperation(), Phases.CLASS_GENERATION)
        return cu
    }

    private class SpockTestAnnotationHarnessOperation extends CompilationUnit.PrimaryClassNodeOperation {
        public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) {
            new SpockTestAnnotationTransform().visit([classNode] as ASTNode[], source);
        }
    }

}
