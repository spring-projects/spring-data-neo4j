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

import com.mysema.query.annotations.*;
import com.mysema.query.apt.DefaultConfiguration;
import com.mysema.query.apt.Processor;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.neo4j.annotation.NodeEntity;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;

/**
 * TODO
 */
@SupportedAnnotationTypes({"com.mysema.query.annotations.*","org.springframework.data.neo4j.annotation.*"})
public class SDNAnnotationProcessor
    extends AbstractProcessor
{
    private static final Boolean ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS = Boolean.FALSE;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Running " + getClass().getSimpleName());
        Class<? extends Annotation> entities = QueryEntities.class;
        Class<? extends Annotation> entity = NodeEntity.class;
        Class<? extends Annotation> superType = QuerySupertype.class;
        Class<? extends Annotation> embeddable = QueryEmbeddable.class;
        Class<? extends Annotation> embedded = QueryEmbedded.class;
        Class<? extends Annotation> skip = QueryTransient.class;

        DefaultConfiguration configuration = new DefaultConfiguration(
                roundEnv, processingEnv.getOptions(), Collections.<String>emptySet(), entities, entity, superType, embeddable, embedded, skip);

        Processor processor = new Processor(processingEnv, roundEnv, configuration);
        processor.process();
        return ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

}
