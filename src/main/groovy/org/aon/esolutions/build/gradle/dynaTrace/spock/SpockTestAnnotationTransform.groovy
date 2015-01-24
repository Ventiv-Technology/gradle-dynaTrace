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
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

@GroovyASTTransformation(phase = CompilePhase.CLASS_GENERATION)
class SpockTestAnnotationTransform implements ASTTransformation {

    public static final Set<MethodNode> FOUND_PROBLEM_METHODS = [];

    public final static ClassNode Specification = ClassHelper.makeWithoutCaching("spock.lang.Specification");
    public final static ClassNode FeatureMetaData = ClassHelper.makeWithoutCaching("org.spockframework.runtime.model.FeatureMetadata");
    public final static ClassNode Test = ClassHelper.makeWithoutCaching("org.junit.Test");

	public void visit(ASTNode[] nodes, SourceUnit sourceUnit) {
        if (nodes && nodes[0] instanceof ClassNode) {
            ClassNode classNode = (ClassNode) nodes[0];
            if (isSpec(classNode)) {
                FOUND_PROBLEM_METHODS.addAll(processSpec(classNode));
            }
        }
	}
	
	List<MethodNode> processSpec(ClassNode clazz) {
		clazz.getMethods().findAll { MethodNode method ->
            return !isMethodVerified(method)
        }
	}

    boolean isMethodVerified(MethodNode method) {
        List<AnnotationNode> featureMetaDataNode = method.getAnnotations(FeatureMetaData);
        if (featureMetaDataNode) {      // This means that it's a Spock Test method
            List<AnnotationNode> testAnnotations = method.getAnnotations(Test);
            return testAnnotations;        // If it is a spock method and has Test annotation, it's good
        }

        return true;
    }

	boolean isSpec(ClassNode clazz) {
		return clazz.isDerivedFrom(Specification);
	}

    public static final List<String> printProblemMethods() {
        List<String> problems = [];

        FOUND_PROBLEM_METHODS.each { MethodNode method ->
            List<AnnotationNode> featureMetaDataNode = method.getAnnotations(FeatureMetaData);
            if (featureMetaDataNode) {
                String testName = ((ConstantExpression)featureMetaDataNode[0].getMember('name')).getValue();
                problems << "Missing @Test Annotation on ${method.getDeclaringClass().getName()}.${testName}"
            }
        }

        return problems;
    }
}
