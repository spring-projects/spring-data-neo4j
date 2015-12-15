/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.querydsl;

import com.querydsl.core.annotations.*;
import com.querydsl.apt.AbstractQuerydslProcessor;
import com.querydsl.apt.Configuration;
import com.querydsl.apt.DefaultConfiguration;
import org.springframework.data.neo4j.annotation.NodeEntity;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic;
import java.util.Collections;

/**
 * TODO
 */
@SupportedAnnotationTypes({"com.querydsl.core.annotations.*","org.springframework.data.neo4j.annotation.*"})
@SuppressWarnings("restriction")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class SDNAnnotationProcessor extends AbstractQuerydslProcessor {

	@Override
	protected Configuration createConfiguration(RoundEnvironment roundEnv) {

		processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Running " + getClass().getSimpleName());

		DefaultConfiguration configuration = new DefaultConfiguration(roundEnv, processingEnv.getOptions(),
				Collections.<String> emptySet(), QueryEntities.class, NodeEntity.class, QuerySupertype.class,
				QueryEmbeddable.class, QueryEmbedded.class, QueryTransient.class);
		// configuration.setUnknownAsEmbedded(true);

		return configuration;
	}
}
