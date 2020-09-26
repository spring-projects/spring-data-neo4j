/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.core.convert;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.ReflectionUtils;

/**
 * This interface needs to be implemented to provide custom configuration for a {@link CustomConversion}. Use cases may
 * be specific date formats or the like. The build method will receive the whole annotation, including all attributes.
 * Classes implementing this interface <strong>must</strong> have a default constructor.
 *
 * @param <A> The type of the annotation
 * @author Michael J. Simons
 * @soundtrack Antilopen Gang - Abwasser
 * @since 6.0
 */
public interface CustomConversionFactory<A extends Annotation> {

	/**
	 * @param config The configuration for building the custom conversion
	 * @return The actual conversion
	 */
	CustomConversion buildConversion(A config, Class<?> typeOfAnnotatedElement);
}
